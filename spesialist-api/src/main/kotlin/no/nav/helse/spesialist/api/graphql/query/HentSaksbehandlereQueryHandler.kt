package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandler

class HentSaksbehandlereQueryHandler(
    private val saksbehandlerDao: SaksbehandlerDao,
) : HentSaksbehandlereQuerySchema {
    override suspend fun hentSaksbehandlere(env: DataFetchingEnvironment): DataFetcherResult<List<ApiSaksbehandler>> {
        val saksbehandlere =
            saksbehandlerDao
                .hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver()
                .map { ApiSaksbehandler(ident = it.ident, navn = it.navn) }
        return byggRespons(saksbehandlere)
    }
}
