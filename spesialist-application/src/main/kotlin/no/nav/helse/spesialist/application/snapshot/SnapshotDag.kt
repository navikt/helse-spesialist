package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotDag(
    val begrunnelser: List<SnapshotBegrunnelse>?,
    val dato: LocalDate,
    val grad: Double?,
    val kilde: SnapshotSykdomsdagkilde,
    val sykdomsdagtype: SnapshotSykdomsdagtype,
    val utbetalingsdagtype: SnapshotUtbetalingsdagType,
    val utbetalingsinfo: SnapshotUtbetalingsinfo?,
)
