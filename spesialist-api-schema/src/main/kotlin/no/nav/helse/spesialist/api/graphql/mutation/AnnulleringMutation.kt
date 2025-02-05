package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiAnnulleringData

interface AnnulleringMutationSchema : Mutation {
    fun annuller(
        annullering: ApiAnnulleringData,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class AnnulleringMutation(private val handler: AnnulleringMutationSchema) : AnnulleringMutationSchema by handler
