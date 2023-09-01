package no.nav.helse.mediator.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class OppdaterPersonService(private val rapidsConnection: RapidsConnection) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun håndter(oppdaterPersonsnapshotDto: OppdaterPersonsnapshotDto) {
        rapidsConnection.publish(
            oppdaterPersonsnapshotDto.fødselsnummer,
            JsonMessage.newMessage(
                "oppdater_personsnapshot", mapOf(
                    "fødselsnummer" to oppdaterPersonsnapshotDto.fødselsnummer
                )
            ).toJson()
        )
        sikkerlogg.info("Publiserte event for å be om siste versjon av person: ${oppdaterPersonsnapshotDto.fødselsnummer}")
    }
}
