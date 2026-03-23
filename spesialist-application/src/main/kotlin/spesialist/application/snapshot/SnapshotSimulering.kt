package no.nav.helse.spesialist.application.snapshot

data class SnapshotSimulering(
    val totalbelop: Int,
    val perioder: List<SnapshotSimuleringsperiode>,
)
