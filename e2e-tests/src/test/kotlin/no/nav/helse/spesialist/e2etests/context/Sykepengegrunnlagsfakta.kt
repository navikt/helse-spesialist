package no.nav.helse.spesialist.e2etests.context

import no.nav.helse.spesialist.domain.testfixtures.jan
import java.time.LocalDate

data class Sykepengegrunnlagsfakta(
    val skjæringstidspunkt: LocalDate = 1 jan 2018,
    val fastsatt: FastsattType = FastsattType.EtterHovedregel,
    val arbeidsgivere: List<Arbeidsgiver>,
) {
    enum class FastsattType {
        EtterHovedregel,
        EtterSkjønn
    }

    open class Arbeidsgiver(
        val organisasjonsnummer: String,
        val omregnetÅrsinntekt: Double,
    ) {
        val inntektskilde: String = "Arbeidsgiver"
    }

    class SkjønnsfastsattArbeidsgiver(
        organisasjonsnummer: String,
        omregnetÅrsinntekt: Double,
        val skjønnsfastsatt: Double,
    ): Arbeidsgiver(organisasjonsnummer, omregnetÅrsinntekt)
}
