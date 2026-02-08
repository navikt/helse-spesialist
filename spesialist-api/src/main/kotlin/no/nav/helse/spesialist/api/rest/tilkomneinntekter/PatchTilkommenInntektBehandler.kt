package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.schema.ApiDatoPeriode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPatchEndring
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektPatch
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PatchBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator
import kotlin.reflect.KProperty0

class PatchTilkommenInntektBehandler : PatchBehandler<TilkomneInntekter.Id, ApiTilkommenInntektPatch, Unit, ApiPatchTilkommenInntektErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: TilkomneInntekter.Id,
        request: ApiTilkommenInntektPatch,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchTilkommenInntektErrorCode> {
        val tilkommenInntekt =
            kallKontekst.transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(resource.tilkommenInntektId))
                ?: return RestResponse.Error(ApiPatchTilkommenInntektErrorCode.FANT_IKKE_TILKOMMEN_INNTEKT)

        return kallKontekst.medPerson(
            identitetsnummer = tilkommenInntekt.identitetsnummer,
            personIkkeFunnet = { error("Personen ble ikke funnet") },
            manglerTilgangTilPerson = { ApiPatchTilkommenInntektErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) {
            behandleForPerson(request, tilkommenInntekt, kallKontekst)
        }
    }

    private fun behandleForPerson(
        request: ApiTilkommenInntektPatch,
        tilkommenInntekt: TilkommenInntekt,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchTilkommenInntektErrorCode> {
        val endringer = request.endringer

        // Valider at alle fra-verdier stemmer med nåværende tilstand
        val harGyldigeFraVerdier =
            sequenceOf(
                fraVerdiValidering(endringer::organisasjonsnummer, tilkommenInntekt.organisasjonsnummer),
                fraVerdiValidering(endringer::periode, tilkommenInntekt.periode) { it.tilPeriode() },
                fraVerdiValidering(endringer::periodebeløp, tilkommenInntekt.periodebeløp),
                fraVerdiValidering(
                    endringer::ekskluderteUkedager,
                    tilkommenInntekt.ekskluderteUkedager,
                ) { it.toSortedSet() },
                fraVerdiValidering(endringer::fjernet, tilkommenInntekt.fjernet),
            ).fold(true) { valid, validering -> valid && validering.valider() }
        if (!harGyldigeFraVerdier) return RestResponse.Error(ApiPatchTilkommenInntektErrorCode.FEIL_UTGANGSPUNKT)

        val tidligerePublisertTilstand = tilkommenInntekt.tilPubliserbarTilstand()

        val saksbehandlerIdent = kallKontekst.saksbehandler.ident
        val notatTilBeslutter = request.notatTilBeslutter
        if (endringer.fjernet?.fra == true && endringer.fjernet?.til == false) {
            // Gjenopprettelse har endringer bakt inn i seg, så vi kaller bare endre hvis vi ikke gjenoppretter samtidig
            gjenopprett(tilkommenInntekt, endringer, saksbehandlerIdent, notatTilBeslutter, kallKontekst.transaksjon)
        } else {
            endre(tilkommenInntekt, endringer, saksbehandlerIdent, notatTilBeslutter, kallKontekst.transaksjon)
        }
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = tilkommenInntekt.periode,
            organisasjonsnummer = tilkommenInntekt.organisasjonsnummer,
            andreTilkomneInntekter =
                kallKontekst.transaksjon.tilkommenInntektRepository
                    .finnAlleForIdentitetsnummer(tilkommenInntekt.identitetsnummer)
                    .minus(tilkommenInntekt),
            vedtaksperioder =
                kallKontekst.transaksjon.legacyVedtaksperiodeRepository.finnVedtaksperioder(
                    tilkommenInntekt.identitetsnummer.value,
                ),
        )

        if (endringer.fjernet?.fra == false && endringer.fjernet?.til == true) {
            fjern(tilkommenInntekt, saksbehandlerIdent, notatTilBeslutter, kallKontekst.transaksjon)
        }

        kallKontekst.transaksjon.tilkommenInntektRepository.lagre(tilkommenInntekt)

        val nåværendeTilstand = tilkommenInntekt.tilPubliserbarTilstand()

        val event =
            InntektsendringerEventBygger.forTilstandsendring(
                tidligerePublisertTilstand = tidligerePublisertTilstand,
                nåværendeTilstand = nåværendeTilstand,
            )

        event?.let {
            kallKontekst.outbox.leggTil(
                identitetsnummer = tilkommenInntekt.identitetsnummer,
                hendelse = it,
                årsak = "endring av tilkommen inntekt",
            )
        }

        loggInfo("Endret tilkommen inntekt", "tilkommenInntektId" to tilkommenInntekt.id)

        return RestResponse.NoContent()
    }

    private fun endre(
        tilkommenInntekt: TilkommenInntekt,
        endringer: ApiTilkommenInntektPatch.ApiTilkommenInntektEndringer,
        saksbehandlerIdent: NAVIdent,
        notatTilBeslutter: String,
        transaksjon: SessionContext,
    ) {
        tilkommenInntekt.endreTil(
            organisasjonsnummer =
                endringer.organisasjonsnummer?.til
                    ?: tilkommenInntekt.organisasjonsnummer,
            periode = endringer.periode?.til?.tilPeriode() ?: tilkommenInntekt.periode,
            periodebeløp = endringer.periodebeløp?.til ?: tilkommenInntekt.periodebeløp,
            ekskluderteUkedager =
                endringer.ekskluderteUkedager?.til?.toSortedSet()
                    ?: tilkommenInntekt.ekskluderteUkedager,
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    identitetsnummer = tilkommenInntekt.identitetsnummer,
                    totrinnsvurderingRepository = transaksjon.totrinnsvurderingRepository,
                ).id(),
        )
    }

    private fun fjern(
        tilkommenInntekt: TilkommenInntekt,
        saksbehandlerIdent: NAVIdent,
        notatTilBeslutter: String,
        transaksjon: SessionContext,
    ) {
        tilkommenInntekt.fjern(
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    identitetsnummer = tilkommenInntekt.identitetsnummer,
                    totrinnsvurderingRepository = transaksjon.totrinnsvurderingRepository,
                ).id(),
        )
    }

    private fun gjenopprett(
        tilkommenInntekt: TilkommenInntekt,
        endringer: ApiTilkommenInntektPatch.ApiTilkommenInntektEndringer,
        saksbehandlerIdent: NAVIdent,
        notatTilBeslutter: String,
        transaksjon: SessionContext,
    ) {
        tilkommenInntekt.gjenopprett(
            organisasjonsnummer =
                endringer.organisasjonsnummer?.til
                    ?: tilkommenInntekt.organisasjonsnummer,
            periode = endringer.periode?.til?.tilPeriode() ?: tilkommenInntekt.periode,
            periodebeløp = endringer.periodebeløp?.til ?: tilkommenInntekt.periodebeløp,
            ekskluderteUkedager =
                endringer.ekskluderteUkedager?.til?.toSortedSet()
                    ?: tilkommenInntekt.ekskluderteUkedager,
            saksbehandlerIdent = saksbehandlerIdent,
            notatTilBeslutter = notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    identitetsnummer = tilkommenInntekt.identitetsnummer,
                    totrinnsvurderingRepository = transaksjon.totrinnsvurderingRepository,
                ).id(),
        )
    }

    private class FraVerdiValidering<T, R>(
        felt: KProperty0<ApiPatchEndring<T>?>,
        private val faktiskVerdi: R,
        private val mapping: (T) -> R,
    ) {
        private val feltverdi = felt.get()
        private val feltnavn = felt.name

        fun valider(): Boolean {
            if (feltverdi != null) {
                val forventetVerdi = mapping(feltverdi.fra)
                if (forventetVerdi != faktiskVerdi) {
                    teamLogs.warn(
                        "Feil / utdatert fra-verdi i request for $feltnavn." +
                            " Requesten forventet $feltnavn=$forventetVerdi," +
                            " men inntekten hadde $feltnavn=$faktiskVerdi",
                    )
                    return false
                }
            }
            return true
        }
    }

    private fun <T, R> fraVerdiValidering(
        endringFelt: KProperty0<ApiPatchEndring<T>?>,
        faktiskVerdi: R,
        mapping: (T) -> R,
    ): FraVerdiValidering<T, R> = FraVerdiValidering(endringFelt, faktiskVerdi, mapping)

    private fun <T> fraVerdiValidering(
        endringFelt: KProperty0<ApiPatchEndring<T>?>,
        faktiskVerdi: T,
    ): FraVerdiValidering<T, T> = FraVerdiValidering(endringFelt, faktiskVerdi) { it }

    private fun TilkommenInntekt.tilPubliserbarTilstand(): InntektsendringerEventBygger.PubliserbarTilstand =
        InntektsendringerEventBygger.PubliserbarTilstand(
            fjernet = fjernet,
            inntektskilde = organisasjonsnummer,
            dagerTilGradering = dagerTilGradering(),
            dagsbeløp = dagbeløp(),
        )

    private fun ApiDatoPeriode.tilPeriode(): Periode = fom tilOgMed tom

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Tilkommen inntekt")
        }
    }
}

enum class ApiPatchTilkommenInntektErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_TILKOMMEN_INNTEKT("Fant ikke tilkommen inntekt", HttpStatusCode.NotFound),
    FEIL_UTGANGSPUNKT("Fra-verdier stemmer ikke med nåværende tilstand", HttpStatusCode.Conflict),
}
