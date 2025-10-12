package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.InntektsendringerEventBygger
import no.nav.helse.spesialist.api.rest.FjernTilkommenInntektRequest
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId

class PostTilkommenInntektFjernBehandler : PostBehandler<TilkomneInntekter.Id.Fjern, FjernTilkommenInntektRequest, Boolean> {
    override fun behandle(
        resource: TilkomneInntekter.Id.Fjern,
        request: FjernTilkommenInntektRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Boolean> {
        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(resource.parent.tilkommenInntektId))
                ?: throw HttpNotFound("Fant ikke tilkommen inntekt med tilkommentInntektId ${resource.parent.tilkommenInntektId}")

        bekreftTilgangTilPerson(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        tilkommenInntekt.fjern(
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = request.notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    fodselsnummer = tilkommenInntekt.fødselsnummer,
                    totrinnsvurderingRepository = transaksjon.totrinnsvurderingRepository,
                ).id(),
        )
        transaksjon.tilkommenInntektRepository.lagre(tilkommenInntekt)

        outbox.leggTil(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forFjernet(tilkommenInntekt),
            årsak = "tilkommen inntekt fjernet",
        )

        return RestResponse.ok(true)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Tilkommen inntekt")
            operationId = operationIdBasertPåKlassenavn()
            request {
                body<FjernTilkommenInntektRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alltid true - henger igjen fra at Apollo ikke tåler å ikke få noe i body"
                    body<Boolean>()
                }
            }
        }
    }
}
