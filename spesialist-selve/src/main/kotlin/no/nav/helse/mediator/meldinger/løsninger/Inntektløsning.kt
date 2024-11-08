package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.PersonRepository
import java.time.LocalDate
import java.time.YearMonth

internal class Inntektløsning(
    private val inntekter: List<Inntekter>,
) {
    internal fun lagre(
        personRepository: PersonRepository,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): Long? = personRepository.lagreInntekter(fødselsnummer, skjæringstidspunkt, inntekter)
}

data class Inntekter(val årMåned: YearMonth, val inntektsliste: List<Inntekt>) {
    data class Inntekt(val beløp: Double, val orgnummer: String)
}
