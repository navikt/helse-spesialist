package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandlerMedOid

interface TildelteOppgaverQuerySchema : Query {
    suspend fun tildelteOppgaverFeed(
        offset: Int,
        limit: Int,
        oppslattSaksbehandler: ApiSaksbehandlerMedOid,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiOppgaverTilBehandling>
}

class TildelteOppgaverQuery(private val handler: TildelteOppgaverQuerySchema) : TildelteOppgaverQuerySchema by handler
