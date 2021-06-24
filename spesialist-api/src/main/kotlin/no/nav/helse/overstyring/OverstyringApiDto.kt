package no.nav.helse.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class OverstyringApiDto(
    val hendelseId: UUID,
    val begrunnelse: String,
    val timestamp: LocalDateTime,
    val overstyrteDager: List<OverstyrtDagApiDto>,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String?,
)

data class OverstyrtDagApiDto(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?
)
