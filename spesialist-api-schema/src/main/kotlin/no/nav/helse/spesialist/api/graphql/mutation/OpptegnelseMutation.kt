package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment

interface OpptegnelseMutationSchema : Mutation {
    fun opprettAbonnement(
        personidentifikator: String,
        environment: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class OpptegnelseMutation(private val handler: OpptegnelseMutationSchema) : OpptegnelseMutationSchema by handler
