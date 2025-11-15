package no.nav.helse.spesialist.application

import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsforholdApiDto

class InMemoryArbeidsgiverApiDao : ArbeidsgiverApiDao {
    override fun finnArbeidsforhold(
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String
    ): List<ArbeidsforholdApiDto> {
        TODO("Not yet implemented")
    }

    override fun finnArbeidsgiverInntekterFraAordningen(
        fødselsnummer: String,
        orgnummer: String
    ): List<ArbeidsgiverApiDao.ArbeidsgiverInntekterFraAOrdningen> {
        TODO("Not yet implemented")
    }
}
