package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotSimuleringsutbetaling(
    val detaljer: List<SnapshotSimuleringsdetaljer>,
    val feilkonto: Boolean,
    val forfall: LocalDate,
    val utbetalesTilId: String,
    val utbetalesTilNavn: String,
)
