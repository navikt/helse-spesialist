package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import java.util.UUID

interface TilkommenInntektMutationSchema : Mutation {
    fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<LeggTilTilkommenInntektResponse>

    fun endreTilkommenInntekt(
        tilkommenInntektId: UUID,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    fun gjenopprettTilkommenInntekt(
        tilkommenInntektId: UUID,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    fun fjernTilkommenInntekt(
        tilkommenInntektId: UUID,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

data class LeggTilTilkommenInntektResponse(val tilkommenInntektId: UUID)

class TilkommenInntektMutation(private val handler: TilkommenInntektMutationSchema) : TilkommenInntektMutationSchema by handler
