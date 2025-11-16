package no.nav.helse.spesialist.application

import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.modell.KomplettArbeidsforholdDto

class UnimplementedArbeidsforholdDao : ArbeidsforholdDao {
    override fun findArbeidsforhold(
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String
    ): List<KomplettArbeidsforholdDto> {
        TODO("Not yet implemented")
    }

    override fun upsertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsforhold: List<KomplettArbeidsforholdDto>
    ) {
        TODO("Not yet implemented")
    }
}
