package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektRequest
import java.util.UUID

interface TilkommenInntektMutationSchema : Mutation {
    fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektRequest,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    fun endreTilkommenInntekt(
        fodselsnummer: String,
        uuid: UUID,
        endretTil: ApiTilkommenInntektRequest,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    fun gjenopprettTilkommenInntekt(
        fodselsnummer: String,
        uuid: UUID,
        endretTil: ApiTilkommenInntektRequest,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    fun fjernTilkommenInntekt(
        fodselsnummer: String,
        uuid: UUID,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class TilkommenInntektMutation(private val handler: TilkommenInntektMutationSchema) : TilkommenInntektMutationSchema by handler
