package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotSimuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<SnapshotSimuleringsutbetaling>,
)
