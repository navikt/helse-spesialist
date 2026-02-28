package no.nav.helse.spesialist.application.snapshot

import java.util.UUID

data class SnapshotUtbetaling(
    val id: UUID,
    val arbeidsgiverFagsystemId: String,
    val arbeidsgiverNettoBelop: Int,
    val personFagsystemId: String,
    val personNettoBelop: Int,
    val statusEnum: SnapshotUtbetalingstatus,
    val typeEnum: SnapshotUtbetalingtype,
    val vurdering: SnapshotVurdering?,
    val personoppdrag: SnapshotOppdrag?,
    val arbeidsgiveroppdrag: SnapshotOppdrag?,
)
