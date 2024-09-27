package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate

interface PersonRepository {
    fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto?

    fun lagreMinimalPerson(minimalPerson: MinimalPersonDto)

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

    fun upsertPersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
    )

    fun finnPersoninfoSistOppdatert(fødselsnummer: String): LocalDate?

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

    fun finnPersoninfoRef(fødselsnummer: String): Long?
}
