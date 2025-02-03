package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiVedtakUtfall

interface TotrinnsvurderingMutationSchema : Mutation {
    suspend fun sendTilGodkjenningV2(
        oppgavereferanse: String,
        vedtakUtfall: ApiVedtakUtfall,
        vedtakBegrunnelse: String? = null,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>

    suspend fun sendIRetur(
        oppgavereferanse: String,
        notatTekst: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean>
}

class TotrinnsvurderingMutation(private val handler: TotrinnsvurderingMutationSchema) :
    TotrinnsvurderingMutationSchema by handler
