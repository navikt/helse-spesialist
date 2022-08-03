package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class OverstyringMediator(
    private val rapidsConnection: RapidsConnection
) {
    internal fun sendOverstyring(packet: JsonNode) {
        val fnr = packet["fødselsnummer"].asText()
        val rawJson = objectMapper.writeValueAsString(packet)
        sikkerLogg.info("Publiserer overstyring for fnr=${fnr}:\n${rawJson}")
        rapidsConnection.publish(fnr, rawJson)
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
