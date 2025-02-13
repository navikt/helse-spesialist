package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDateTime

data class SnapshotOppdrag(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val utbetalingslinjer: List<SnapshotUtbetalingslinje>,
    val simulering: SnapshotSimulering?,
)
