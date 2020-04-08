package no.nav.helse.modell.dto

import java.time.LocalDate
import java.util.*

data class VedtakDto(
    val id: Long,
    val vedtaksperiodeId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val arbeidsgiverRef: Long,
    val personRef: Long,
    val speilSnapshotRef: Long
)
