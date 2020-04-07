package no.nav.helse.modell

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.*

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

class Behov(val typer: List<Behovtype>, val fødselsnummer: String, val orgnummer: String, val spleisBehovId: UUID, val vedtaksperiodeId: UUID) {
    fun toJson() = objectMapper.writeValueAsString(mapOf(
        "@behov" to typer,
        "@id" to UUID.randomUUID(),
        "@opprettet" to LocalDateTime.now(),
        "spleisBehovId" to spleisBehovId,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "fødselsnummer" to fødselsnummer,
        "orgnummer" to orgnummer
    ))
}

enum class Behovtype{
    HentEnhet,
    HentPersoninfo,
    HentArbeidsgiverNavn
}
