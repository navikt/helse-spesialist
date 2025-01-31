package no.nav.helse.db.api

import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsforholdApiDto
import java.time.YearMonth

interface ArbeidsgiverApiDao {
    fun finnBransjer(organisasjonsnummer: String): List<String>

    fun finnNavn(organisasjonsnummer: String): String?

    fun finnArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ): List<ArbeidsforholdApiDto>

    fun finnArbeidsgiverInntekterFraAordningen(
        fødselsnummer: String,
        orgnummer: String,
    ): List<ArbeidsgiverInntekterFraAOrdningen>

    data class Inntekter(
        val årMåned: YearMonth,
        val inntektsliste: List<Inntekt>,
    ) {
        data class Inntekt(val beløp: Double, val orgnummer: String)
    }

    data class ArbeidsgiverInntekterFraAOrdningen(
        val skjaeringstidspunkt: String,
        val inntekter: List<InntektFraAOrdningen>,
    )

    data class InntektFraAOrdningen(
        val maned: YearMonth,
        val sum: Double,
    )
}
