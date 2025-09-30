package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandler
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import kotlin.reflect.typeOf

class GetAktiveSaksbehandlereHåndterer : GetHåndterer<Unit, List<ApiSaksbehandler>> {
    override val urlPath = "aktive-saksbehandlere"

    override fun extractParametre(parameters: Parameters) = Unit

    override fun håndter(
        urlParametre: Unit,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<List<ApiSaksbehandler>> =
        RestResponse.ok(
            transaksjon.saksbehandlerDao
                .hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver()
                .map { ApiSaksbehandler(ident = it.ident, navn = it.navn) },
        )

    override val urlParametersClass = Unit::class

    override val responseBodyType = typeOf<List<ApiSaksbehandler>>()
}
