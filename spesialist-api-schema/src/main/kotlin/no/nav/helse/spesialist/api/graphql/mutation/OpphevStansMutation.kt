package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment

interface OpphevStansMutationSchema : Mutation {
    suspend fun opphevStans(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean>
}

class OpphevStansMutation(private val handler: OpphevStansMutationSchema) : OpphevStansMutationSchema by handler
