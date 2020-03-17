package no.nav.helse.modell.oppgave

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

class Behov(val typer: List<Behovtype>, val fødselsnummer: String, val orgnummer: String, val spleisBehovId: UUID) {
    fun toJson() = objectMapper.writeValueAsString(mapOf(
        "@behov" to typer,
        "@id" to UUID.randomUUID(),
        "spleisBehovId" to spleisBehovId,
        "fødselsnummer" to fødselsnummer,
        "orgnummer" to orgnummer
    ))
}

enum class Behovtype{HENT_ENHET, HENT_NAVN}
