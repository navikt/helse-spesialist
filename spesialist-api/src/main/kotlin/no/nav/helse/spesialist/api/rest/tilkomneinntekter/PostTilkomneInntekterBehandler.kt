package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import no.nav.helse.spesialist.api.rest.ApiLeggTilTilkommenInntektRequest
import no.nav.helse.spesialist.api.rest.ApiLeggTilTilkommenInntektResponse
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PersonErrorCode
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PostTilkomneInntekterBehandler : PostBehandler<TilkomneInntekter, ApiLeggTilTilkommenInntektRequest, ApiLeggTilTilkommenInntektResponse, PersonErrorCode> {
    override val tag = Tags.TILKOMNE_INNTEKTER

    override fun behandle(
        resource: TilkomneInntekter,
        request: ApiLeggTilTilkommenInntektRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiLeggTilTilkommenInntektResponse, PersonErrorCode> =
        kallKontekst.medPerson(
            identitetsnummer = Identitetsnummer.fraString(identitetsnummer = request.fodselsnummer),
        ) { person ->
            behandleForPerson(request, person, kallKontekst)
        }

    private fun behandleForPerson(
        request: ApiLeggTilTilkommenInntektRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiLeggTilTilkommenInntektResponse, PersonErrorCode> {
        val periode = request.verdier.periode.fom tilOgMed request.verdier.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = periode,
            organisasjonsnummer = request.verdier.organisasjonsnummer,
            andreTilkomneInntekter =
                kallKontekst.transaksjon.tilkommenInntektRepository.finnAlleForIdentitetsnummer(person.id),
            vedtaksperioder =
                kallKontekst.transaksjon.legacyVedtaksperiodeRepository.finnVedtaksperioder(person.id.value),
        )

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
