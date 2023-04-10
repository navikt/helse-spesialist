package no.nav.helse.mediator.api

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection

class OppdaterPersonService(val rapidsConnection: RapidsConnection) {

    fun håndter(oppdaterPersonsnapshotDto: OppdaterPersonsnapshotDto) {
        rapidsConnection.publish(
            oppdaterPersonsnapshotDto.fødselsnummer,
            JsonMessage.newMessage(
                "oppdater_personsnapshot", mapOf(
                    "fødselsnummer" to oppdaterPersonsnapshotDto.fødselsnummer
                )
            ).toJson()
        )
        sikkerLogg.info("Publiserte event for å be om siste versjon av person: ${oppdaterPersonsnapshotDto.fødselsnummer}")
    }

}
