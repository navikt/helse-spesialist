package no.nav.helse.db

import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.Adressebeskyttelse
import java.time.LocalDate

interface PersonDao {
    fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto?

    fun finnInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): List<Inntekter>?

    fun lagreInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ): Long?

    fun finnPersonMedFødselsnummer(fødselsnummer: String): Long?

    fun finnEnhetId(fødselsnummer: String): String

    fun finnAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse?
}
