package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall

interface VedtakMutationSchema : Mutation {
    fun fattVedtak(
        oppgavereferanse: String,
        env: DataFetchingEnvironment,
        utfall: ApiVedtakUtfall? = null,
        begrunnelse: String? = null,
    ): DataFetcherResult<Boolean>

    fun sendTilInfotrygd(
        oppgavereferanse: String,
        arsak: String,
        begrunnelser: List<String>,
        kommentar: String?,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class VedtakMutation(private val handler: VedtakMutationSchema) : VedtakMutationSchema by handler
