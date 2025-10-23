package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.resources.Vedtak
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostFattVedtakBehandler : PostBehandler<Vedtak.Id.Fatt, Unit, Boolean> {
    override fun behandle(
        resource: Vedtak.Id.Fatt,
        request: Unit,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Boolean> {
        val behandling =
            transaksjon.behandlingRepository.finn(SpleisBehandlingId(resource.parent.behandlingId))
                ?: throw HttpNotFound("Fant ikke behandling med behandlingId ${resource.parent.behandlingId}")
        bekreftTilgangTilPerson(
            fødselsnummer = behandling.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )
        return RestResponse(HttpStatusCode.OK, false)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = listOf("Vedtak")
            operationId = operationIdBasertPåKlassenavn()
            response {
                code(HttpStatusCode.OK) {
                    body<Boolean>()
                }
            }
        }
    }
}
