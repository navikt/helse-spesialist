package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.graphql.schema.ApiKommentar
import no.nav.helse.spesialist.api.graphql.schema.ApiNotat
import no.nav.helse.spesialist.api.graphql.schema.ApiNotatType

interface NotatMutationSchema : Mutation {
    fun feilregistrerNotat(id: Int): DataFetcherResult<ApiNotat?>

    fun feilregistrerKommentar(id: Int): DataFetcherResult<ApiKommentar?>

    fun feilregistrerKommentarV2(id: Int): DataFetcherResult<ApiKommentar?>

    fun leggTilNotat(
        tekst: String,
        type: ApiNotatType,
        vedtaksperiodeId: String,
        saksbehandlerOid: String,
    ): DataFetcherResult<ApiNotat?>

    fun leggTilKommentar(
        dialogRef: Int,
        tekst: String,
        saksbehandlerident: String,
    ): DataFetcherResult<ApiKommentar?>
}

class NotatMutation(
    private val handler: NotatMutationSchema,
) : NotatMutationSchema by handler
