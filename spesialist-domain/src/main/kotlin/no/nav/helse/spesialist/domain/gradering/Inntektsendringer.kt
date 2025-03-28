package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.spesialist.domain.Periode

data class Inntektsendringer(
    val organisasjonsnummer: String,
    val nyeEllerEndredeInntekter: List<PeriodeMedBeløp>,
    val fjernedeInntekter: List<Periode>,
) {
    data class PeriodeMedBeløp(
        val periode: Periode,
        val dagbeløp: Double,
    )
}
