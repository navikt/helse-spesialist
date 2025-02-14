package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.PersonDao
import java.time.LocalDate
import java.time.YearMonth

class Inntektløsning(
    private val inntekter: List<Inntekter>,
) {
    fun lagre(
        personDao: PersonDao,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): Long? = personDao.lagreInntekter(fødselsnummer, skjæringstidspunkt, inntekter)
}

data class Inntekter(val årMåned: YearMonth, val inntektsliste: List<Inntekt>) {
    data class Inntekt(val beløp: Double, val orgnummer: String)
}
