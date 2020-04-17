package no.nav.helse.modell.dto

import no.nav.helse.Oppgavestatus
import java.time.LocalDateTime
import java.util.UUID

data class OppgaveDto(
    val id: Long,
    val oppdatert: LocalDateTime?,
    val oppgaveType: String,
    val behovId: UUID,
    val status: Oppgavestatus,
    val vedtaksref: Long?
)
