package no.nav.helse.db

import no.nav.helse.modell.KomplettArbeidsforholdDto

interface ArbeidsforholdDao {
    fun findArbeidsforhold(
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String,
    ): List<KomplettArbeidsforholdDto>

    fun upsertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsforhold: List<KomplettArbeidsforholdDto>,
    )
}
