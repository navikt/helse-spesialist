package no.nav.helse.spesialist.domain.inntektsperiode

import java.time.LocalDate

data class Inntektsendringer(
    val organisasjonsnummer: String,
    val nyeEllerEndredeInntekter: List<PeriodeMedBeløp>,
    val fjernedeInntekter: List<PeriodeUtenBeløp>,
) {
    data class PeriodeMedBeløp(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagbeløp: Double,
    ) {
        fun hengerSammenMed(dag: Inntektsdag) = tom.plusDays(1) == dag.dato && dagbeløp == dag.beløp
    }

    data class PeriodeUtenBeløp(
        val fom: LocalDate,
        val tom: LocalDate,
    ) {
        fun hengerSammenMed(dag: Inntektsdag) = tom.plusDays(1) == dag.dato
    }
}
