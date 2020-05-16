package no.nav.helse.modell

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.*

class Behov(
    val typer: List<Behovtype>,
    val fødselsnummer: String,
    val orgnummer: String?,
    val spleisBehovId: UUID,
    val vedtaksperiodeId: UUID?
) {
    fun toJson() = JsonMessage.newMessage(mutableMapOf(
        "@event_name" to "behov",
        "@behov" to typer,
        "@id" to UUID.randomUUID(),
        "@opprettet" to LocalDateTime.now(),
        "spleisBehovId" to spleisBehovId,
        "fødselsnummer" to fødselsnummer
    ).apply {
        vedtaksperiodeId?.also { this["vedtaksperiodeId"] = it }
        orgnummer?.also { this["orgnummer"] = it }
    }).toJson()
}

enum class Behovtype {
    HentEnhet,
    HentPersoninfo,
    HentArbeidsgiverNavn
}
