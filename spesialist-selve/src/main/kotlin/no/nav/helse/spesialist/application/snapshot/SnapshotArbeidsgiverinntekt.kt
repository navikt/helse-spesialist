package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: SnapshotOmregnetArsinntekt,
    val skjonnsmessigFastsatt: SnapshotSkjonnsmessigFastsatt?,
    val deaktivert: Boolean?,
    val fom: LocalDate,
    val tom: LocalDate?,
)
