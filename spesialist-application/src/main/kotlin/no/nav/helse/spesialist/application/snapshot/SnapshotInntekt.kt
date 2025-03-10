package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotInntekt(
    val inntektskilde: String,
    val periodiserteInntekter: List<PeriodisertInntekt>,
) {
    data class PeriodisertInntekt(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagligBelop: Double,
    )
}
