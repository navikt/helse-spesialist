package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotUtbetalingslinje(
    val tom: LocalDate,
    val fom: LocalDate,
    val grad: Int,
    val dagsats: Int,
)
