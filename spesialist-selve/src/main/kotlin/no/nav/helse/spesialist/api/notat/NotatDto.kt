package no.nav.helse.spesialist.api.notat

import java.time.LocalDateTime

data class KommentarDto(
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerident: String,
    val feilregistrertTidspunkt: LocalDateTime?,
)
