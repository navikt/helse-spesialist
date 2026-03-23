package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment

interface TotrinnsvurderingMutationSchema : Mutation {
    fun sendTilGodkjenningV2(
        oppgavereferanse: String,
        vedtakBegrunnelse: String? = null,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?>

    fun sendIRetur(
        oppgavereferanse: String,
        notatTekst: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Boolean?>
}

class TotrinnsvurderingMutation(
    private val handler: TotrinnsvurderingMutationSchema,
) : TotrinnsvurderingMutationSchema by handler
