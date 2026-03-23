package no.nav.helse.modell.arbeidsforhold

import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.modell.KomplettArbeidsforholdDto
import java.time.LocalDate
import java.time.LocalDateTime

class Arbeidsforholdløsning(
    private val løsninger: List<Løsning>,
) {
    data class Løsning(
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
        val stillingstittel: String,
        val stillingsprosent: Int,
    )

    fun upsert(
        arbeidsforholdDao: ArbeidsforholdDao,
        fødselsnummer: String,
        organisasjonsnummer: String,
        oppdatert: LocalDateTime = LocalDateTime.now(),
    ) {
        arbeidsforholdDao.upsertArbeidsforhold(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforhold =
                løsninger.map {
                    KomplettArbeidsforholdDto(
                        fødselsnummer = fødselsnummer,
                        organisasjonsnummer = organisasjonsnummer,
                        startdato = it.startdato,
                        sluttdato = it.sluttdato,
                        stillingstittel = it.stillingstittel,
                        stillingsprosent = it.stillingsprosent,
                        oppdatert = oppdatert,
                    )
                },
        )
    }
}
