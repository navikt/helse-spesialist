package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.graphql.schema.Kommentar
import no.nav.helse.spesialist.api.graphql.schema.Notat
import no.nav.helse.spesialist.api.graphql.schema.Notater
import no.nav.helse.spesialist.api.notat.KommentarDto
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatDto

class NotatQuery(private val notatDao: NotatDao) : Query {

    @Suppress("unused")
    suspend fun notater(forPerioder: List<String>): DataFetcherResult<List<Notater>> {
        if (forPerioder.isEmpty()) {
            return DataFetcherResult.newResult<List<Notater>>()
                .error(getEmptyListError()).build()
        }

        val ids = try {
            forPerioder.map(UUID::fromString)
        } catch (_: Exception) {
            return DataFetcherResult.newResult<List<Notater>>()
                .error(getBadUUIDStringError()).build()
        }

        val notater = withContext(Dispatchers.IO) {
            notatDao.finnNotater(ids)
        }.tilNotater()

        return DataFetcherResult.newResult<List<Notater>>()
            .data(notater).build()
    }

    private fun getBadUUIDStringError(): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Requesten inneholder UUIDer på feil format")
        .extensions(mapOf("code" to 400))
        .build()

    private fun getEmptyListError(): GraphQLError = GraphqlErrorException.newErrorException()
        .message("Requesten mangler liste med vedtaksperiode-ider")
        .extensions(mapOf("code" to 400))
        .build()

}

private fun Map<UUID, List<NotatDto>>.tilNotater(): List<Notater> =
    toList().map { (id, notater) ->
        Notater(
            id = id.toString(),
            notater = notater.map(::tilNotat)
        )
    }

internal fun tilNotat(notat: NotatDto) = Notat(
    id = notat.id,
    tekst = notat.tekst,
    opprettet = notat.opprettet.format(DateTimeFormatter.ISO_DATE_TIME),
    saksbehandlerOid = notat.saksbehandlerOid.toString(),
    saksbehandlerNavn = notat.saksbehandlerNavn,
    saksbehandlerEpost = notat.saksbehandlerEpost,
    saksbehandlerIdent = notat.saksbehandlerIdent,
    vedtaksperiodeId = notat.vedtaksperiodeId.toString(),
    feilregistrert = notat.feilregistrert,
    feilregistrert_tidspunkt = notat.feilregistrert_tidspunkt?.format(DateTimeFormatter.ISO_DATE_TIME),
    type = notat.type,
    kommentarer = notat.kommentarer.map(::tilKommentar)
)

internal fun tilKommentar(kommentar: KommentarDto) = Kommentar(
    id = kommentar.id,
    tekst = kommentar.tekst,
    opprettet = kommentar.opprettet.toString(),
    saksbehandlerident = kommentar.saksbehandlerident,
    feilregistrert_tidspunkt = kommentar.feilregistrertTidspunkt?.toString(),
)
