package no.nav.helse.sidegig

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
)
