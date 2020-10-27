package no.nav.helse.modell.egenansatt

import java.time.LocalDateTime

class EgenAnsattDto(
    val fødselsnummer: String,
    val erEgenAnsatt: Boolean,
    val opprettet: LocalDateTime
)
