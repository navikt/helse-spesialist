package no.nav.helse.modell.gosysoppgaver

import java.time.LocalDateTime

class ÅpneGosysOppgaverDto(
    val fødselsnummer: String,
    val antall: Int?,
    val oppslagFeilet: Boolean,
    val opprettet: LocalDateTime
)
