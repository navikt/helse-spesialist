package no.nav.helse.db

import no.nav.helse.modell.KomplettArbeidsforholdDto

interface ArbeidsforholdRepository {
    fun findArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ): List<KomplettArbeidsforholdDto>

    fun upsertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsforhold: List<KomplettArbeidsforholdDto>,
    )
}
