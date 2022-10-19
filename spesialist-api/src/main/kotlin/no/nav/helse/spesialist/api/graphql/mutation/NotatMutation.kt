package no.nav.helse.spesialist.api.graphql.mutation

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import java.time.format.DateTimeFormatter
import no.nav.helse.spesialist.api.graphql.schema.Kommentar
import no.nav.helse.spesialist.api.notat.NotatDao

class NotatMutation(private val notatDao: NotatDao) : Mutation {

    @Suppress("unused")
    fun feilregistrerNotat(id: Int): Boolean {
        val antallOppdatert = notatDao.feilregistrerNotat(id)
        return antallOppdatert > 0
    }

    @Suppress("unused")
    fun feilregistrerKommentar(id: Int): Boolean {
        val antallOppdatert = notatDao.feilregistrerKommentar(id)
        return antallOppdatert > 0
    }

    @Suppress("unused")
    fun leggTilKommentar(notatId: Int, tekst: String, saksbehandlerident: String): DataFetcherResult<Kommentar?> {
        val kommentar = try {
            notatDao.leggTilKommentar(notatId, tekst, saksbehandlerident) ?: return DataFetcherResult.newResult<Kommentar?>().data(null).build()
        } catch (e: Exception) {
            return DataFetcherResult.newResult<Kommentar?>().error(getNotFoundError(notatId)).build()
        }

        val result = Kommentar(
            id = kommentar.id,
            tekst = kommentar.tekst,
            opprettet = kommentar.opprettet.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            saksbehandlerident = kommentar.saksbehandlerident,
            feilregistrert_tidspunkt = null,
        )

        return DataFetcherResult.newResult<Kommentar?>().data(result).build()
    }

    private fun getNotFoundError(notatId: Int): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Finner ikke notat med id $notatId")
        .extensions(mapOf("code" to 404))
        .build()
}