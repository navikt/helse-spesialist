package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.schema.ApiAktivSaksbehandler
import no.nav.helse.spesialist.api.rest.resources.AktiveSaksbehandlere
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class GetAktiveSaksbehandlereBehandler : GetBehandler<AktiveSaksbehandlere, List<ApiAktivSaksbehandler>> {
    override fun behandle(
        resource: AktiveSaksbehandlere,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<List<ApiAktivSaksbehandler>> =
        RestResponse.ok(
            transaksjon.saksbehandlerDao
                .hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver()
                .map { ApiAktivSaksbehandler(ident = it.ident, navn = it.navn, oid = it.id().value) },
        )

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Saksbehandlere")
            operationId = operationIdBasertPåKlassenavn()
            response {
                code(HttpStatusCode.OK) {
                    description = "Liste med aktive saksbehandlere"
                    body<List<ApiAktivSaksbehandler>>()
                }
            }
        }
    }
}
