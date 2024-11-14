package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import no.nav.helse.spesialist.api.graphql.query.tilKommentar
import no.nav.helse.spesialist.api.graphql.query.tilNotat
import no.nav.helse.spesialist.api.graphql.schema.Kommentar
import no.nav.helse.spesialist.api.graphql.schema.Notat
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatApiDao
import java.util.UUID

class NotatMutation(
    private val notatDao: NotatApiDao,
) : Mutation {
    @Suppress("unused")
    fun feilregistrerNotat(id: Int): DataFetcherResult<Notat?> {
        val notatDto =
            try {
                notatDao.feilregistrerNotat(id)
            } catch (e: RuntimeException) {
                return DataFetcherResult.newResult<Notat?>().error(kunneIkkeFeilregistrereNotatError(id)).build()
            }
        return DataFetcherResult.newResult<Notat?>().data(notatDto?.let(::tilNotat)).build()
    }

    @Suppress("unused")
    fun feilregistrerKommentar(id: Int): DataFetcherResult<Kommentar?> {
        val kommentarDto =
            try {
                notatDao.feilregistrerKommentar(id)
            } catch (e: Exception) {
                return DataFetcherResult
                    .newResult<Kommentar?>()
                    .error(kunneIkkeFeilregistrereKommentarError(id))
                    .build()
            }
        return DataFetcherResult.newResult<Kommentar?>().data(kommentarDto?.let(::tilKommentar)).build()
    }

    @Suppress("unused")
    fun feilregistrerKommentarV2(id: Int): DataFetcherResult<Kommentar?> = feilregistrerKommentar(id)

    @Suppress("unused")
    fun leggTilNotat(
        tekst: String,
        type: NotatType,
        vedtaksperiodeId: String,
        saksbehandlerOid: String,
    ): DataFetcherResult<Notat?> {
        val vedtaksperiodeIdUUID = UUID.fromString(vedtaksperiodeId)
        val notatDto =
            try {
                notatDao.opprettNotat(vedtaksperiodeIdUUID, tekst, UUID.fromString(saksbehandlerOid), type)
            } catch (e: RuntimeException) {
                return DataFetcherResult
                    .newResult<Notat?>()
                    .error(kunneIkkeOppretteNotatError(vedtaksperiodeIdUUID))
                    .build()
            }
        return DataFetcherResult.newResult<Notat?>().data(notatDto?.let(::tilNotat)).build()
    }

    @Suppress("unused")
    fun leggTilKommentar(
        dialogRef: Int,
        tekst: String,
        saksbehandlerident: String,
    ): DataFetcherResult<Kommentar?> {
        val kommentarDto =
            try {
                notatDao.leggTilKommentar(dialogRef, tekst, saksbehandlerident)
            } catch (e: Exception) {
                return DataFetcherResult
                    .newResult<Kommentar?>()
                    .error(kunneIkkeLeggeTilKommentar(dialogRef))
                    .build()
            }

        return DataFetcherResult.newResult<Kommentar?>().data(kommentarDto?.let(::tilKommentar)).build()
    }

    @Suppress("unused")
    fun leggTilKommentarMedDialogRef(
        dialogRef: Int,
        tekst: String,
        saksbehandlerident: String,
    ): DataFetcherResult<Kommentar?> {
        val kommentarDto =
            try {
                notatDao.leggTilKommentar(dialogRef, tekst, saksbehandlerident)
            } catch (e: Exception) {
                return DataFetcherResult
                    .newResult<Kommentar?>()
                    .error(kunneIkkeLeggeTilKommentar(dialogRef))
                    .build()
            }

        return DataFetcherResult.newResult<Kommentar?>().data(kommentarDto?.let(::tilKommentar)).build()
    }

    private fun kunneIkkeLeggeTilKommentar(dialogRef: Int): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message("Kunne ikke legge til kommentar med dialog-ref: $dialogRef")
            .extensions(mapOf("code" to 500))
            .build()

    private fun kunneIkkeFeilregistrereNotatError(id: Int): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message("Kunne ikke feilregistrere notat med id $id")
            .extensions(mapOf("code" to 500))
            .build()

    private fun kunneIkkeFeilregistrereKommentarError(id: Int): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message("Kunne ikke feilregistrere kommentar med id $id")
            .extensions(mapOf("code" to 500))
            .build()

    private fun kunneIkkeOppretteNotatError(id: UUID): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message("Kunne ikke opprette notat for vedtaksperiode med id $id")
            .extensions(mapOf("code" to 500))
            .build()
}
