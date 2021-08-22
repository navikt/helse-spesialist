package no.nav.helse.notat

import java.time.LocalDateTime
import java.util.*

data class NotatDto (
    val id: Int,
    val tekst: String,
    val opprettet: LocalDateTime,
    val saksbehandlerOid: UUID,
    val saksbehandlerNavn: String,
    val saksbehandlerEpost: String,
    val oppgaveRef: Int
)
