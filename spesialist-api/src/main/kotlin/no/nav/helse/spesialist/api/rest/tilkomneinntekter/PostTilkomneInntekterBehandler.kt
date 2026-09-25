package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PostTilkomneInntekterBehandler : PostBehandler<TilkomneInntekter, ApiLeggTilTilkommenInntektRequest, ApiLeggTilTilkommenInntektResponse, ApiPostTilkomneInntekterErrorCode> {
    override val tag = Tags.TILKOMNE_INNTEKTER

    override fun behandle(
        resource: TilkomneInntekter,
        request: ApiLeggTilTilkommenInntektRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiLeggTilTilkommenInntektResponse, ApiPostTilkomneInntekterErrorCode> =
        kallKontekst.medPerson(
            identitetsnummer = Identitetsnummer.fraString(identitetsnummer = request.fodselsnummer),
            personIkkeFunnet = { ApiPostTilkomneInntekterErrorCode.PERSON_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostTilkomneInntekterErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(request, person, kallKontekst)
        }

    private fun behandleForPerson(
        request: ApiLeggTilTilkommenInntektRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiLeggTilTilkommenInntektResponse, ApiPostTilkomneInntekterErrorCode> {
        val periode = request.verdier.periode.fom tilOgMed request.verdier.periode.tom
        val valideringResultat =
            TilkommenInntektPeriodeValidator.validerPeriode(
                periode = periode,
                organisasjonsnummer = request.verdier.organisasjonsnummer,
                andreTilkomneInntekter =
                    kallKontekst.transaksjon.tilkommenInntektRepository.finnAlleForIdentitetsnummer(person.id),
                behandlinger = kallKontekst.alleGjeldendeBehandlingerForPerson(person.id),
            )

        when (valideringResultat) {
            is TilkommenInntektPeriodeValidator.Resultat.OK -> {}
            is TilkommenInntektPeriodeValidator.Resultat.GårUtenforSykefraværstilfelle ->
                return RestResponse.Error(
                    ApiPostTilkomneInntekterErrorCode.GÅR_UTENFOR_SYKEFRAVÆRSTILFELLE,
                )

            is TilkommenInntektPeriodeValidator.Resultat.OverlapperAnnenTilkommenInntekt ->
                return RestResponse.Error(
                    ApiPostTilkomneInntekterErrorCode.OVERLAPPER_ANNEN_TILKOMMEN_INNTEKT,
                )
        }

        val tilkommenInntekt =
            TilkommenInntekt.ny(
                identitetsnummer = person.id,
                saksbehandlerIdent = kallKontekst.saksbehandler.ident,
                notatTilBeslutter = request.notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        identitetsnummer = person.id,
                        totrinnsvurderingRepository = kallKontekst.transaksjon.totrinnsvurderingRepository,
                    ).id(),
                organisasjonsnummer = request.verdier.organisasjonsnummer,
                periode = periode,
                periodebeløp = request.verdier.periodebelop,
                ekskluderteUkedager = request.verdier.ekskluderteUkedager.toSet(),
            )
        kallKontekst.transaksjon.tilkommenInntektRepository.lagre(tilkommenInntekt)

        kallKontekst.outbox.leggTil(
            identitetsnummer = tilkommenInntekt.identitetsnummer,
            hendelse =
                InntektsendringerEventBygger.forNy(
                    inntektskilde = tilkommenInntekt.organisasjonsnummer,
                    dagerTilGradering = tilkommenInntekt.dagerTilGradering(),
                    dagsbeløp = tilkommenInntekt.dagbeløp(),
                ),
            årsak = "tilkommen inntekt lagt til",
        )

        loggInfo("La til tilkommen inntekt", "tilkommenInntektId" to tilkommenInntekt.id)

        return RestResponse.OK(ApiLeggTilTilkommenInntektResponse(tilkommenInntekt.id.value))
    }
}

enum class ApiPostTilkomneInntekterErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.BadRequest),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    GÅR_UTENFOR_SYKEFRAVÆRSTILFELLE(
        "Kan ikke legge til tilkommen inntekt som går utenfor et sykefraværstilfelle",
        HttpStatusCode.BadRequest,
    ),
    OVERLAPPER_ANNEN_TILKOMMEN_INNTEKT(
        "Kan ikke legge til tilkommen inntekt som overlapper med en annen tilkommen inntekt for samme inntektskilde",
        HttpStatusCode.BadRequest,
    ),
}
