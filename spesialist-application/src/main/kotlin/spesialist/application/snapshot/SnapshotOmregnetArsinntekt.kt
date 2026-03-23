package no.nav.helse.spesialist.application.snapshot

data class SnapshotOmregnetArsinntekt(
    val belop: Double,
    val inntekterFraAOrdningen: List<SnapshotInntekterFraAOrdningen>?,
    val kilde: SnapshotInntektskilde,
    val manedsbelop: Double,
)
