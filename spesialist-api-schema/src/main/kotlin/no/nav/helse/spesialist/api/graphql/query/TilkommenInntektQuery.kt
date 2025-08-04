package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektskilde

interface TilkommenInntektQuerySchema : Query {
    suspend fun tilkomneInntektskilder(
        aktorId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<List<ApiTilkommenInntektskilde>>

    suspend fun tilkomneInntektskilderV2(
        fodselsnummer: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<List<ApiTilkommenInntektskilde>>
}

class TilkommenInntektQuery(
    private val handler: TilkommenInntektQuerySchema,
) : TilkommenInntektQuerySchema by handler
