package no.nav.helse.modell.dkif

import java.time.LocalDateTime

class DigitalKontaktinformasjonDto(
    val fødselsnummer: String,
    val erDigital: Boolean,
    val opprettet: LocalDateTime
)
