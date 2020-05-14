package no.nav.helse.modell

import no.nav.helse.objectMapper
import java.time.LocalDateTime
import java.util.*

class Behov(
    val typer: List<Behovtype>,
    val fødselsnummer: String,
    val orgnummer: String?,
    val spleisBehovId: UUID,
    val vedtaksperiodeId: UUID?
) {
    fun toJson() = objectMapper.writeValueAsString(
        mapOf(
            "@event_name" to "behov",
            "@behov" to typer,
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "spleisBehovId" to spleisBehovId,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "fødselsnummer" to fødselsnummer,
            "orgnummer" to orgnummer
        )
    )
}

enum class Behovtype {
    HentEnhet,
    HentPersoninfo,
    HentArbeidsgiverNavn,
    HentInfotrygdutbetalinger
}
