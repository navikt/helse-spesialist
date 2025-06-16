package no.nav.helse.db.api

import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsforholdApiDto
import no.nav.helse.spesialist.domain.ArbeidsgiverId
import java.time.YearMonth

interface ArbeidsgiverApiDao {
    fun finnArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsgiverRef: ArbeidsgiverId,
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
