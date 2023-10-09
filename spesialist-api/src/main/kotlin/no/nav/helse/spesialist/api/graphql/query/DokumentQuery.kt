package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import com.fasterxml.jackson.databind.JsonNode
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.graphql.schema.Soknad

class DokumentQuery(private val dokumenthåndterer: Dokumenthåndterer) : Query {
    @Suppress("unused")
    suspend fun hentSoknad(fnr: String, dokumentId: String): DataFetcherResult<Soknad> {
        if (dokumentId.isEmpty()) {
            return DataFetcherResult.newResult<Soknad>().error(getEmptyRequestError()).build()
        }

        val dokument = withContext(Dispatchers.IO) {
            dokumenthåndterer.håndter(fnr, UUID.fromString(dokumentId))
        }.tilSøknad()

        return DataFetcherResult.newResult<Soknad>().data(dokument).build()
    }

    private fun getEmptyRequestError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Requestem mangler dokument-id")
            .extensions(mapOf("code" to 400)).build()

    private fun JsonNode.tilSøknad(): Soknad {
        return Soknad(sendtNav = LocalDateTime.now().toString(), soknadsperioder = listOf())
    }
}