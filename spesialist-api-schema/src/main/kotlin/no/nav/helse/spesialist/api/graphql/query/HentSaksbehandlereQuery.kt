package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandler

interface HentSaksbehandlereQuerySchema : Query {
    suspend fun hentSaksbehandlere(env: DataFetchingEnvironment): DataFetcherResult<List<ApiSaksbehandler>>
}

class HentSaksbehandlereQuery(private val handler: HentSaksbehandlereQuerySchema) :
    HentSaksbehandlereQuerySchema by handler
