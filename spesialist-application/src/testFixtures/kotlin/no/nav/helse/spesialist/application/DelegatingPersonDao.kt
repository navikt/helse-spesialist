package no.nav.helse.spesialist.application

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.Adressebeskyttelse
import java.time.LocalDate

class DelegatingPersonDao(
    private val personRepository: InMemoryPersonRepository,
    private val inntektRepository: InMemoryInntektRepository,
) : PersonDao {
    override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto? =
        personRepository.alle().find { it.id.value == fødselsnummer }?.let {
            MinimalPersonDto(fødselsnummer = fødselsnummer, aktørId = it.aktørId)
        }

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String): LocalDate? =
        personRepository.alle().find { it.id.value == fødselsnummer }?.infotrygdutbetalingerOppdatert

    override fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ) = -1L

    override fun finnInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): List<Inntekter>? = inntektRepository.finn(fødselsnummer, skjæringstidspunkt)

    override fun lagreInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ): Long? {
        if (personRepository.alle().none { it.id.value == fødselsnummer }) return null
        inntektRepository.lagre(fødselsnummer, skjæringstidspunkt, inntekter)
        return fødselsnummer.hashCode().toLong()
    }

    override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long? =
        if (personRepository.alle().any { it.id.value == fødselsnummer })
            fødselsnummer.hashCode().toLong()
        else
            null

    override fun finnEnhetId(fødselsnummer: String): String {
        val enhetRef = personRepository.alle().find { it.id.value == fødselsnummer }?.enhetRef
        checkNotNull(enhetRef) { "Fant ikke enhet for $fødselsnummer" }
        return if (enhetRef < 1000) "0$enhetRef" else "$enhetRef"
    }

    override fun finnAdressebeskyttelse(fødselsnummer: String) =
        personRepository.alle().find { it.id.value == fødselsnummer }?.info?.adressebeskyttelse?.let {
            when (it) {
                no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.Ugradert -> Adressebeskyttelse.Ugradert
                no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.Fortrolig -> Adressebeskyttelse.Fortrolig
                no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.StrengtFortrolig -> Adressebeskyttelse.StrengtFortrolig
                no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.StrengtFortroligUtland -> Adressebeskyttelse.StrengtFortroligUtland
                no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.Ukjent -> Adressebeskyttelse.Ukjent
            }
        }
}

