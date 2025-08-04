package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiDokumentInntektsmelding
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknad

interface DokumentQuerySchema : Query {
    suspend fun hentSoknad(
        fnr: String,
        dokumentId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiSoknad?>

    suspend fun hentInntektsmelding(
        fnr: String,
        dokumentId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiDokumentInntektsmelding?>
}

class DokumentQuery(
    private val handler: DokumentQuerySchema,
) : DokumentQuerySchema by handler
