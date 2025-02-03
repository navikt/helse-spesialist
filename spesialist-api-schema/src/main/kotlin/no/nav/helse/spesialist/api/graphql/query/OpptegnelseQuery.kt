package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiOpptegnelse

interface OpptegnelseQuerySchema : Query {
    fun opptegnelser(
        sekvensId: Int? = null,
        environment: DataFetchingEnvironment,
    ): DataFetcherResult<List<ApiOpptegnelse>>
}

class OpptegnelseQuery(private val handler: OpptegnelseQuerySchema) : OpptegnelseQuerySchema by handler
