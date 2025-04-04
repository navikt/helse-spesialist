package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring
import java.util.UUID

interface TilkommenInntektMutationSchema : Mutation {
    fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektOverstyring,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Unit>

    fun endreTilkommenInntekt(
        fodselsnummer: String,
        uuid: UUID,
        endretTil: ApiTilkommenInntektOverstyring,
        notatTilBeslutter: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Unit>
}

class TilkommenInntektMutation(private val handler: TilkommenInntektMutationSchema) : TilkommenInntektMutationSchema by handler
