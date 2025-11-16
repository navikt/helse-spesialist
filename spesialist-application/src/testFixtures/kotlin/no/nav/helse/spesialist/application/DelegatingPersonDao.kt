package no.nav.helse.spesialist.application

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate

class DelegatingPersonDao(private val personRepository: InMemoryPersonRepository) : PersonDao {
    override fun personKlargjort(fødselsnummer: String) {
        TODO("Not yet implemented")
    }

    override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto? {
        TODO("Not yet implemented")
    }

    override fun lagreMinimalPerson(minimalPerson: MinimalPersonDto) {
        TODO("Not yet implemented")
    }

    override fun finnEnhetSistOppdatert(fødselsnummer: String): LocalDate? {
        TODO("Not yet implemented")
    }

    override fun oppdaterEnhet(fødselsnummer: String, enhetNr: Int): Int {
        TODO("Not yet implemented")
    }

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String): LocalDate? {
        TODO("Not yet implemented")
    }

    override fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode
    ) = -1L

    override fun upsertPersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse
    ) {
        personRepository.alle().first { it.id.value == fødselsnummer }.oppdaterInfo(
            no.nav.helse.spesialist.domain.Personinfo(
                fornavn = fornavn,
                mellomnavn = mellomnavn,
                etternavn = etternavn,
                fødselsdato = fødselsdato,
                kjønn = when (kjønn) {
                    Kjønn.Mann -> no.nav.helse.spesialist.domain.Personinfo.Kjønn.Mann
                    Kjønn.Kvinne -> no.nav.helse.spesialist.domain.Personinfo.Kjønn.Kvinne
                    Kjønn.Ukjent -> no.nav.helse.spesialist.domain.Personinfo.Kjønn.Ukjent
                },
                adressebeskyttelse = when (adressebeskyttelse) {
                    Adressebeskyttelse.Ugradert -> no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.Ugradert
                    Adressebeskyttelse.Fortrolig -> no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.Fortrolig
                    Adressebeskyttelse.StrengtFortrolig -> no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.StrengtFortrolig
                    Adressebeskyttelse.StrengtFortroligUtland -> no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.StrengtFortroligUtland
                    Adressebeskyttelse.Ukjent -> no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.Ukjent
                }
            )
        )
    }

    override fun finnPersoninfoSistOppdatert(fødselsnummer: String): LocalDate? {
        TODO("Not yet implemented")
    }

    override fun finnInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate
    ): List<Inntekter>? {
        TODO("Not yet implemented")
    }

    override fun lagreInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>
    ): Long? {
        TODO("Not yet implemented")
    }

    override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long? {
        TODO("Not yet implemented")
    }

    override fun finnPersoninfoRef(fødselsnummer: String): Long? {
        TODO("Not yet implemented")
    }

    override fun finnEnhetId(fødselsnummer: String): String {
        TODO("Not yet implemented")
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

    override fun finnAktørId(fødselsnummer: String): String? {
        TODO("Not yet implemented")
    }

    override fun insertPerson(
        fødselsnummer: String,
        aktørId: String,
        personinfoId: Long,
        enhetId: Int,
        infotrygdutbetalingerId: Long
    ): Long {
        TODO("Not yet implemented")
    }
}
