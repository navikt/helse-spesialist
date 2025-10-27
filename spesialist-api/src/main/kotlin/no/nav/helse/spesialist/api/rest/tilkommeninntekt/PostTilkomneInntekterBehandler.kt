package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.InntektsendringerEventBygger
import no.nav.helse.spesialist.api.rest.ApiLeggTilTilkommenInntektRequest
import no.nav.helse.spesialist.api.rest.ApiLeggTilTilkommenInntektResponse
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PostTilkomneInntekterBehandler : PostBehandler<TilkomneInntekter, ApiLeggTilTilkommenInntektRequest, ApiLeggTilTilkommenInntektResponse> {
    override fun behandle(
        resource: TilkomneInntekter,
        request: ApiLeggTilTilkommenInntektRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<ApiLeggTilTilkommenInntektResponse> {
        bekreftTilgangTilPerson(
            fødselsnummer = request.fodselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        val periode = request.verdier.periode.fom tilOgMed request.verdier.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = periode,
            organisasjonsnummer = request.verdier.organisasjonsnummer,
            andreTilkomneInntekter = transaksjon.tilkommenInntektRepository.finnAlleForFødselsnummer(request.fodselsnummer),
            vedtaksperioder = transaksjon.legacyVedtaksperiodeRepository.finnVedtaksperioder(request.fodselsnummer),
        )

        val tilkommenInntekt =
            TilkommenInntekt.ny(
                fødselsnummer = request.fodselsnummer,
                saksbehandlerIdent = saksbehandler.ident,
                notatTilBeslutter = request.notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        fodselsnummer = request.fodselsnummer,
                        totrinnsvurderingRepository = transaksjon.totrinnsvurderingRepository,
                    ).id(),
                organisasjonsnummer = request.verdier.organisasjonsnummer,
                periode = periode,
                periodebeløp = request.verdier.periodebelop,
                ekskluderteUkedager = request.verdier.ekskluderteUkedager.toSet(),
            )
        transaksjon.tilkommenInntektRepository.lagre(tilkommenInntekt)

        outbox.leggTil(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forNy(tilkommenInntekt),
            årsak = "tilkommen inntekt lagt til",
        )

        return RestResponse.created(ApiLeggTilTilkommenInntektResponse(tilkommenInntekt.id().value))
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Tilkommen inntekt")
            operationId = operationIdBasertPåKlassenavn()
            request {
                body<ApiLeggTilTilkommenInntektRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Svar med ID på den opprettede tilkomne inntekten"
                    body<ApiLeggTilTilkommenInntektResponse>()
                }
            }
        }
    }
}
