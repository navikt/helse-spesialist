package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate
import java.util.UUID

data class SnapshotGhostPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlagId: UUID,
    val deaktivert: Boolean,
)
