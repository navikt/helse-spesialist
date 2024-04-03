package no.nav.helse.modell.arbeidsforhold

import java.time.LocalDate

internal class Arbeidsforholdløsning(
    private val løsninger: List<Løsning>,
) {
    data class Løsning(
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
        val stillingstittel: String,
        val stillingsprosent: Int,
    )

    internal fun opprett(
        arbeidsforholdDao: ArbeidsforholdDao,
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) = løsninger.forEach {
        arbeidsforholdDao.insertArbeidsforhold(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            startdato = it.startdato,
            sluttdato = it.sluttdato,
            stillingstittel = it.stillingstittel,
            stillingsprosent = it.stillingsprosent,
        )
    }

    internal fun oppdater(
        personDao: ArbeidsforholdDao,
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) {
        personDao.oppdaterArbeidsforhold(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforhold = løsninger,
        )
    }
}
