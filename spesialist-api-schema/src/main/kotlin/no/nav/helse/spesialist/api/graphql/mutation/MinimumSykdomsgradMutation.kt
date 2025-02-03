package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiMinimumSykdomsgrad

interface MinimumSykdomsgradMutationSchema : Mutation {
    suspend fun minimumSykdomsgrad(
        minimumSykdomsgrad: ApiMinimumSykdomsgrad,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class MinimumSykdomsgradMutation(private val handler: MinimumSykdomsgradMutationSchema) :
    MinimumSykdomsgradMutationSchema by handler
