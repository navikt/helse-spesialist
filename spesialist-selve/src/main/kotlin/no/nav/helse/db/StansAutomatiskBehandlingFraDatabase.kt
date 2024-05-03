package no.nav.helse.db

import java.time.LocalDateTime

data class StansAutomatiskBehandlingFraDatabase(
    val fødselsnummer: String,
    val status: String,
    val årsaker: Set<String>,
    val opprettet: LocalDateTime,
)
