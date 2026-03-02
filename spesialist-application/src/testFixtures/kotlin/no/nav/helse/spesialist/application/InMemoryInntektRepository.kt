package no.nav.helse.spesialist.application

import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import java.time.LocalDate

class InMemoryInntektRepository {
    private val inntekter = mutableMapOf<Pair<String, LocalDate>, List<Inntekter>>()

    fun lagre(fødselsnummer: String, skjæringstidspunkt: LocalDate, inntekterListe: List<Inntekter>) {
        inntekter[fødselsnummer to skjæringstidspunkt] = inntekterListe
    }

    fun finn(fødselsnummer: String, skjæringstidspunkt: LocalDate): List<Inntekter>? =
        inntekter[fødselsnummer to skjæringstidspunkt]
}
