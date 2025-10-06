package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.schema.ApiAktivSaksbehandler
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import kotlin.reflect.typeOf

class GetAktiveSaksbehandlereHåndterer : GetHåndterer<Unit, List<ApiAktivSaksbehandler>> {
    override val urlPath = "aktive-saksbehandlere"

    override fun extractParametre(
        pathParameters: Parameters,
        queryParameters: Parameters,
    ) = Unit

    override fun håndter(
        urlParametre: Unit,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<List<ApiAktivSaksbehandler>> =
        RestResponse.ok(
            transaksjon.saksbehandlerDao
                .hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver()
                .map { ApiAktivSaksbehandler(ident = it.ident, navn = it.navn, oid = it.id().value) },
        )

    override val urlParametersClass = Unit::class

    override val responseBodyType = typeOf<List<ApiAktivSaksbehandler>>()
}
