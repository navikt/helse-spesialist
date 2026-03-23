package no.nav.helse.spesialist.application.snapshot

data class SnapshotUtbetalingsinfo(
    val arbeidsgiverbelop: Int?,
    val inntekt: Int?,
    val personbelop: Int?,
    val refusjonsbelop: Int?,
    val totalGrad: Double?,
    val utbetaling: Int?,
)
