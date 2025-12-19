package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.Adressebeskyttelse
import java.time.LocalDate

interface PersonDao {
    fun personKlargjort(fødselsnummer: String)

    fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto?

    fun finnEnhetSistOppdatert(fødselsnummer: String): LocalDate?

    fun oppdaterEnhet(
        fødselsnummer: String,
        enhetNr: Int,
    ): Int

    fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String): LocalDate?

    fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ): Long

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
