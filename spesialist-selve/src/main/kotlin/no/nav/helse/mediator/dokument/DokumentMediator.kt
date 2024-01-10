package no.nav.helse.mediator.dokument

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.Dokumenthåndterer
import org.slf4j.LoggerFactory

class DokumentMediator(
    private val dokumentDao: DokumentDao,
    private val rapidsConnection: RapidsConnection,
    private val retries: Int = 30,
) : Dokumenthåndterer {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun håndter(fødselsnummer: String, dokumentId: UUID, dokumentType: String): JsonNode {
        return dokumentDao.hent(fødselsnummer, dokumentId).let { dokument ->
            val erTom = dokument?.size() == 0
            val error = dokument?.path("error")?.takeUnless { it.isMissingOrNull() }?.asInt()
            if (dokument == null || erTom || error != 404) {
                sendHentDokument(fødselsnummer, dokumentId, dokumentType)

                val response = runBlocking {
                    delay(100)
                    hentDokument(fødselsnummer, dokumentId, retries)
                }
                return@let response
            }

            return@let dokument
        }
    }

    private suspend fun hentDokument(fødselsnummer: String, dokumentId: UUID, retries: Int): JsonNode {
        if (retries == 0) return objectMapper.createObjectNode()

        val response = runBlocking {
            val dokument = dokumentDao.hent(fødselsnummer, dokumentId)
            if (dokument == null) {
                delay(100)
                hentDokument(fødselsnummer, dokumentId, retries - 1)
            } else {
                return@runBlocking dokument
            }
        }
        return response
    }

    private fun sendHentDokument(
        fødselsnummer: String, dokumentId: UUID, dokumentType: String
    ) {
        val message = JsonMessage.newMessage(
            "hent-dokument", mapOf(
                "fødselsnummer" to fødselsnummer,
                "dokumentId" to dokumentId,
                "dokumentType" to dokumentType
            )
        )
        sikkerlogg.info(
            "Publiserer hent-dokument med {}, {}",
            StructuredArguments.kv("dokumentId", dokumentId),
            StructuredArguments.kv("dokumentType", dokumentType),
        )
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }
}
