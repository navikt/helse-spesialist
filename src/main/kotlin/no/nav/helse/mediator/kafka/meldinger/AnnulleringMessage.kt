package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.*

class AnnulleringMessage(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val saksbehandler: String
) {
    fun toJson() = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "kanseller_utbetaling",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "aktørId" to aktørId,
            "fagsystemId" to fagsystemId,
            "saksbehandler" to saksbehandler
        )
    ).toJson()
}
