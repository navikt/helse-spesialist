package no.nav.helse.spesialist.application

import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.modell.KomplettArbeidsforholdDto

class InMemoryArbeidsforholdDao : ArbeidsforholdDao {
    val eksisterendeArbeidsforhold = mutableListOf<KomplettArbeidsforholdDto>()
    val oppdaterteArbeidsforhold = mutableListOf<KomplettArbeidsforholdDto>()

    override fun findArbeidsforhold(
        fødselsnummer: String,
        arbeidsgiverIdentifikator: String,
    ): List<KomplettArbeidsforholdDto> =
        eksisterendeArbeidsforhold.filter {
            it.fødselsnummer == fødselsnummer && it.organisasjonsnummer == arbeidsgiverIdentifikator
        }

    override fun upsertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsforhold: List<KomplettArbeidsforholdDto>,
    ) {
        oppdaterteArbeidsforhold.addAll(arbeidsforhold)
        eksisterendeArbeidsforhold.removeIf {
            it.fødselsnummer == fødselsnummer && it.organisasjonsnummer == organisasjonsnummer
        }
        eksisterendeArbeidsforhold.addAll(arbeidsforhold)
    }
}
