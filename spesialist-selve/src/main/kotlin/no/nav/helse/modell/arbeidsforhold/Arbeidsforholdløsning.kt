package no.nav.helse.modell.arbeidsforhold

import no.nav.helse.db.ArbeidsforholdRepository
import no.nav.helse.modell.KomplettArbeidsforholdDto
import java.time.LocalDate

class Arbeidsforholdløsning(
    private val løsninger: List<Løsning>,
) {
    data class Løsning(
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
        val stillingstittel: String,
        val stillingsprosent: Int,
    )

    internal fun upsert(
        arbeidsforholdRepository: ArbeidsforholdRepository,
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) {
        arbeidsforholdRepository.upsertArbeidsforhold(
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
                    )
                },
        )
    }
}
