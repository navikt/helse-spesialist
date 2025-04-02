package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring

interface TilkommenInntektMutationSchema : Mutation {
    fun leggTilTilkommenInntekt(
        tilkommenInntektOverstyring: ApiTilkommenInntektOverstyring,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class TilkommenInntektMutation(private val handler: TilkommenInntektMutationSchema) : TilkommenInntektMutationSchema by handler
