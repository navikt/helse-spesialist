package no.nav.helse.modell.dto

import no.nav.helse.Løsningstype
import java.time.LocalDateTime
import java.util.UUID

data class OppgaveDto(
    val id: Long,
    val ferdigstilt: LocalDateTime?,
    val oppgaveType: String,
    val behovId: UUID,
    val løsningstype: Løsningstype,
    val vedtaksref: Long?
)
