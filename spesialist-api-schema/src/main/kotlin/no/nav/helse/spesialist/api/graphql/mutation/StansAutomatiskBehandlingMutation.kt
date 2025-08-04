package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment

interface StansAutomatiskBehandlingMutationSchema : Mutation {
    fun stansAutomatiskBehandling(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean>

    fun opphevStansAutomatiskBehandling(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean>
}

class StansAutomatiskBehandlingMutation(
    private val handler: StansAutomatiskBehandlingMutationSchema,
) : StansAutomatiskBehandlingMutationSchema by handler
