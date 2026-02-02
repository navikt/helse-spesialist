package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiLeggTilTilkommenInntektRequest
import no.nav.helse.spesialist.api.rest.ApiLeggTilTilkommenInntektResponse
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.finnEllerOpprettTotrinnsvurdering
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PostTilkomneInntekterBehandler : PostBehandler<TilkomneInntekter, ApiLeggTilTilkommenInntektRequest, ApiLeggTilTilkommenInntektResponse, ApiPostTilkomneInntekterErrorCode> {
    override fun behandle(
        resource: TilkomneInntekter,
        request: ApiLeggTilTilkommenInntektRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiLeggTilTilkommenInntektResponse, ApiPostTilkomneInntekterErrorCode> {
        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = request.fodselsnummer),
                brukerroller = kallKontekst.brukerroller,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostTilkomneInntekterErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val periode = request.verdier.periode.fom tilOgMed request.verdier.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = periode,
            organisasjonsnummer = request.verdier.organisasjonsnummer,
            andreTilkomneInntekter = kallKontekst.transaksjon.tilkommenInntektRepository.finnAlleForFødselsnummer(request.fodselsnummer),
            vedtaksperioder = kallKontekst.transaksjon.legacyVedtaksperiodeRepository.finnVedtaksperioder(request.fodselsnummer),
        )

        val tilkommenInntekt =
            TilkommenInntekt.ny(
                fødselsnummer = request.fodselsnummer,
                saksbehandlerIdent = kallKontekst.saksbehandler.ident,
                notatTilBeslutter = request.notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        fodselsnummer = request.fodselsnummer,
                        totrinnsvurderingRepository = kallKontekst.transaksjon.totrinnsvurderingRepository,
                    ).id(),
                organisasjonsnummer = request.verdier.organisasjonsnummer,
                periode = periode,
                periodebeløp = request.verdier.periodebelop,
                ekskluderteUkedager = request.verdier.ekskluderteUkedager.toSet(),
            )
        kallKontekst.transaksjon.tilkommenInntektRepository.lagre(tilkommenInntekt)

        kallKontekst.outbox.leggTil(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse =
                InntektsendringerEventBygger.forNy(
                    inntektskilde = tilkommenInntekt.organisasjonsnummer,
                    dagerTilGradering = tilkommenInntekt.dagerTilGradering(),
                    dagsbeløp = tilkommenInntekt.dagbeløp(),
                ),
            årsak = "tilkommen inntekt lagt til",
        )

        return RestResponse.OK(ApiLeggTilTilkommenInntektResponse(tilkommenInntekt.id.value))
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Tilkommen inntekt")
        }
    }
}

enum class ApiPostTilkomneInntekterErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
