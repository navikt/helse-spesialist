package no.nav.helse.modell.person

import DatabaseIntegrationTest
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class PersonDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagre personinfo`() {
        personDao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
        assertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
    }

    @Test
    fun `ingen mellomnavn`() {
        personDao.insertPersoninfo(FORNAVN, null, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
        assertPersoninfo(FORNAVN, null, ETTERNAVN, FØDSELSDATO, KJØNN, ADRESSEBESKYTTELSE)
    }

    @Test
    fun `lagre infotrygdutbetalinger`() {
        personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        assertEquals(1, infotrygdUtbetalinger().size)
    }

    @Test
    fun `oppretter person`() {
        val (_, personinfoId, enhetId, infotrygdutbetalingerId) = opprettPerson()
        assertNotNull(personDao.findPersonByFødselsnummer(FNR))
        assertNotNull(personDao.findInfotrygdutbetalinger(FNR))
        assertEquals(LocalDate.now(), personDao.findEnhetSistOppdatert(FNR))
        assertEquals(LocalDate.now(), personDao.findITUtbetalingsperioderSistOppdatert(FNR))
        assertEquals(LocalDate.now(), personDao.findPersoninfoSistOppdatert(FNR))
        assertEquals(1, person().size)
        person().first().assertEquals(FNR, AKTØR, personinfoId, enhetId, infotrygdutbetalingerId)
    }

    @Test
    fun `oppretter minimal person`() {
        opprettMinimalPerson()
        assertNotNull(personDao.findPersonByFødselsnummer(FNR))
        assertNull(personDao.findEnhetSistOppdatert(FNR))
        assertNull(personDao.findITUtbetalingsperioderSistOppdatert(FNR))
        assertNull(personDao.findPersoninfoSistOppdatert(FNR))
        assertEquals(1, person().size)
        person().first().assertEquals(FNR, AKTØR, null, null, null)
    }

    @Test
    fun `oppdaterer personinfo`() {
        opprettPerson()
        val nyttFornavn = "OLE"
        val nyttMellomnavn = "PETTER"
        val nyttEtternavn = "SVENSKE"
        val nyFødselsdato = LocalDate.of(1990, 12, 31)
        val nyttKjønn = Kjønn.Mann
        val nyAdressebeskyttelse = Adressebeskyttelse.Fortrolig
        personDao.upsertPersoninfo(FNR, nyttFornavn, nyttMellomnavn, nyttEtternavn, nyFødselsdato, nyttKjønn, nyAdressebeskyttelse)
        assertPersoninfo(nyttFornavn, nyttMellomnavn, nyttEtternavn, nyFødselsdato, nyttKjønn, nyAdressebeskyttelse)
    }

    @Test
    fun `finner enhet`() {
        opprettPerson()
        assertEquals(ENHET.toInt(), personDao.finnEnhetId(FNR).toInt())
    }

    @Test
    fun `oppdaterer enhet`() {
        opprettPerson()
        val nyEnhet = "2100".toInt()
        personDao.updateEnhet(FNR, nyEnhet)
        person().first().assertEnhet(nyEnhet)
    }

    @Test
    fun `oppdaterer infotrygdutbetalinger`() {
        opprettPerson()
        assertEquals("{}", infotrygdUtbetalinger().first())
        val utbetalinger = objectMapper.createObjectNode().set<JsonNode>("test", objectMapper.createArrayNode())
        personDao.upsertInfotrygdutbetalinger(FNR, utbetalinger)
        assertEquals( "{\"test\":[]}", infotrygdUtbetalinger().first())
    }

    @Test
    fun `finner adressebeskyttelse for person`() {
        opprettPerson(fødselsnummer = FNR, adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        assertEquals(Adressebeskyttelse.Fortrolig, personDao.findAdressebeskyttelse(FNR))
    }

    private fun assertPersoninfo(
        forventetNavn: String,
        forventetMellomnavn: String?,
        forventetEtternavn: String?,
        forventetFødselsdato: LocalDate,
        forventetKjønn: Kjønn,
        forventetAdressebeskyttelse: Adressebeskyttelse
    ) {
        val personinfo = personinfo()
        assertEquals(1, personinfo.size)
        personinfo.first()
            .assertEquals(forventetNavn, forventetMellomnavn, forventetEtternavn, forventetFødselsdato, forventetKjønn, forventetAdressebeskyttelse)
    }

    private fun infotrygdUtbetalinger() =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf("SELECT data FROM infotrygdutbetalinger")
                    .map { row -> row.string("data") }
                    .asList
            )
        }

    private fun person() = sessionOf(dataSource).use { session ->
        session.run(
            queryOf("SELECT fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref FROM person")
                .map { row ->
                    Person(
                        row.long("fodselsnummer").toFødselsnummer(),
                        row.long("aktor_id").toString(),
                        row.longOrNull("info_ref"),
                        row.intOrNull("enhet_ref"),
                        row.longOrNull("infotrygdutbetalinger_ref")
                    )
                }
                .asList
        )
    }

    private fun personinfo() =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf("SELECT fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse FROM person_info")
                    .map { row ->
                        Personinfo(
                            row.string("fornavn"),
                            row.stringOrNull("mellomnavn"),
                            row.string("etternavn"),
                            row.localDate("fodselsdato"),
                            enumValueOf(row.string("kjonn")),
                            enumValueOf(row.string("adressebeskyttelse"))
                        )
                    }
                    .asList
            )
        }

    private class Person(
        private val fødselsnummer: String,
        private val aktørId: String,
        private val infoRef: Long?,
        private val enhetRef: Int?,
        private val infotrygdutbetalingerRef: Long?
    ) {
        fun assertEquals(
            forventetFødselsnummer: String,
            forventetAktørId: String,
            forventetInfoRef: Long?,
            forventetEnhetRef: Int?,
            forventetInfotrygdutbetalingerRef: Long?
        ) {
            assertEquals(forventetFødselsnummer, fødselsnummer)
            assertEquals(forventetAktørId, aktørId)
            assertEquals(forventetInfoRef, infoRef)
            assertEquals(forventetEnhetRef, enhetRef)
            assertEquals(forventetInfotrygdutbetalingerRef, infotrygdutbetalingerRef)
        }

        fun assertEnhet(forventetEnhet: Int) {
            assertEquals(forventetEnhet, enhetRef)
        }
    }

    private class Personinfo(
        private val fornavn: String,
        private val mellomnavn: String?,
        private val etternavn: String,
        private val fødselsdato: LocalDate,
        private val kjønn: Kjønn,
        private val adressebeskyttelse: Adressebeskyttelse
    ) {
        fun assertEquals(
            forventetNavn: String,
            forventetMellomnavn: String?,
            forventetEtternavn: String?,
            forventetFødselsdato: LocalDate,
            forventetKjønn: Kjønn,
            forventetAdressebeskyttelse: Adressebeskyttelse
        ) {
            assertEquals(forventetNavn, fornavn)
            assertEquals(forventetMellomnavn, mellomnavn)
            assertEquals(forventetEtternavn, etternavn)
            assertEquals(forventetFødselsdato, fødselsdato)
            assertEquals(forventetKjønn, kjønn)
            assertEquals(forventetAdressebeskyttelse, adressebeskyttelse)
        }
    }
}
