package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.*

data class OppgaveDto(
    val id: Long,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime?,
    val oppgaveType: String,
    val eventId: UUID,
    val status: Oppgavestatus,
    val vedtaksref: Long?
)
