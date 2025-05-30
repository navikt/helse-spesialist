package no.nav.helse.spesialist.application.snapshot

data class SnapshotArbeidsgiver(
    val organisasjonsnummer: String,
    val ghostPerioder: List<SnapshotGhostPeriode>,
    val generasjoner: List<SnapshotGenerasjon>,
)
