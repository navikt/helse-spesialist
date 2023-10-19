package no.nav.helse.spesialist.api.graphql.query

import com.fasterxml.jackson.databind.JsonNode
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.schema.Soknad
import no.nav.helse.spesialist.api.person.PersonApiDao

class DokumentQuery(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    private val dokumenthåndterer: Dokumenthåndterer,
) : AbstractPersonQuery(personApiDao, egenAnsattApiDao) {
    @Suppress("unused")
    suspend fun hentSoknad(fnr: String, dokumentId: String, env: DataFetchingEnvironment): DataFetcherResult<Soknad> {
        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<Soknad?>().error(getForbiddenError(fnr)).build()
        }

        if (dokumentId.isEmpty()) {
            return DataFetcherResult.newResult<Soknad>().error(getEmptyRequestError()).build()
        }

        val dokument = withContext(Dispatchers.IO) {
            dokumenthåndterer.håndter(fnr, UUID.fromString(dokumentId), DokumentType.SØKNAD.name)
        }.let {
            if (it.size() == 0) return DataFetcherResult.newResult<Soknad>().error(getEmptyResultTimeoutError()).build()
            return@let it.tilSøknad()
        }

        return DataFetcherResult.newResult<Soknad>().data(dokument).build()
    }

    private fun getEmptyRequestError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Requesten mangler dokument-id")
            .extensions(mapOf("code" to 400)).build()

    private fun getEmptyResultTimeoutError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Noe gikk galt, vennligst prøv igjen.")
            .extensions(mapOf("code" to 408)).build()

    private fun JsonNode.tilSøknad(): Soknad {
        return Soknad(
            sendtNav = this["sendtNav"].asText(),
            soknadsperioder = listOf()
        )
    }
}

enum class DokumentType {
    SØKNAD, INNTEKTSMELDING
}