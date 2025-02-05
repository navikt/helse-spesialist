package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiTildeling

interface TildelingMutationSchema : Mutation {
    fun opprettTildeling(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiTildeling?>

    fun fjernTildeling(
        oppgaveId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class TildelingMutation(private val handler: TildelingMutationSchema) : TildelingMutationSchema by handler
