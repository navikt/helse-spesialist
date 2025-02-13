package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate
import java.util.UUID

data class SnapshotNyttInntektsforholdPeriode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val organisasjonsnummer: String,
    val skjaeringstidspunkt: LocalDate,
    val dagligBelop: Double,
    val manedligBelop: Double,
)
