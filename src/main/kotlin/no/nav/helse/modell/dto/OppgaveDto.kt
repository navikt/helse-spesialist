package no.nav.helse.modell.dto

import java.time.LocalDateTime
import java.util.UUID

data class OppgaveDto(
    val id: Long,
    val ferdigstilt: LocalDateTime?,
    val oppgaveType: String,
    val behovId: UUID
)
