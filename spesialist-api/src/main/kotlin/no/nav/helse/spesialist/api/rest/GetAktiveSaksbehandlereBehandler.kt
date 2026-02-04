package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import no.nav.helse.spesialist.api.graphql.schema.ApiAktivSaksbehandler
import no.nav.helse.spesialist.api.rest.resources.AktiveSaksbehandlere
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetAktiveSaksbehandlereBehandler : GetBehandler<AktiveSaksbehandlere, List<ApiAktivSaksbehandler>, ApiGetAktiveSaksbehandlereErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: AktiveSaksbehandlere,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiAktivSaksbehandler>, ApiGetAktiveSaksbehandlereErrorCode> =
        RestResponse.OK(
            kallKontekst.transaksjon.saksbehandlerDao
                .hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver()
                .map { ApiAktivSaksbehandler(ident = it.ident.value, navn = it.navn, oid = it.id.value) },
        )

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Saksbehandlere")
        }
    }
}

enum class ApiGetAktiveSaksbehandlereErrorCode : ApiErrorCode
