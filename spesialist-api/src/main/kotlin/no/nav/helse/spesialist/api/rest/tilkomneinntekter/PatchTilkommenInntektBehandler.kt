package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import io.ktor.http.*
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PatchTilkommenInntektBehandler : PatchBehandler<TilkomneInntekter.Id, ApiTilkommenInntektPatch, Unit, ApiPatchTilkommenInntektErrorCode> {
    override val tag = Tags.TILKOMNE_INNTEKTER

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
                fraVerdiValidering(
                    endringer::periode,
                    tilkommenInntekt.periode,
                ) { it.tilPeriode() },
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
            loggInfo("Behandler forespørselen som gjenoppretting av tilkommen inntekt", "tilkommenInntektId" to tilkommenInntekt.id)
            gjenopprett(tilkommenInntekt, endringer, saksbehandlerIdent, notatTilBeslutter, kallKontekst.transaksjon)
        } else {
            loggInfo("Behandler forespørselen som endring av tilkommen inntekt", "tilkommenInntektId" to tilkommenInntekt.id)
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

    private fun TilkommenInntekt.tilPubliserbarTilstand(): InntektsendringerEventBygger.PubliserbarTilstand =
        InntektsendringerEventBygger.PubliserbarTilstand(
            fjernet = fjernet,
            inntektskilde = organisasjonsnummer,
            dagerTilGradering = dagerTilGradering(),
            dagsbeløp = dagbeløp(),
        )

    private fun ApiDatoPeriode.tilPeriode(): Periode = fom tilOgMed tom
}

enum class ApiPatchTilkommenInntektErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    FANT_IKKE_TILKOMMEN_INNTEKT("Fant ikke tilkommen inntekt", HttpStatusCode.NotFound),
    FEIL_UTGANGSPUNKT("Fra-verdier stemmer ikke med nåværende tilstand", HttpStatusCode.Conflict),
}
