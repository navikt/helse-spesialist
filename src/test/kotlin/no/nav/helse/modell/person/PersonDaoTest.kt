package no.nav.helse.modell.person

import AbstractEndToEndTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PersonDaoTest : AbstractEndToEndTest() {
    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
        private const val FORNAVN = "KARI"
        private const val MELLOMNAVN = "Mellomnavn"
        private const val ETTERNAVN = "Nordmann"
        private const val ENHET_OSLO = "0301"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Kvinne

        private val objectMapper = jacksonObjectMapper()
    }

    private lateinit var dao: PersonDao

    @BeforeEach
    fun setup() {
        dao = PersonDao(dataSource)
    }

    @Test
    fun `lagre personinfo`() {
        dao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        assertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
    }

    @Test
    fun `ingen mellomnavn`() {
        dao.insertPersoninfo(FORNAVN, null, ETTERNAVN, FØDSELSDATO, KJØNN)
        assertPersoninfo(FORNAVN, null, ETTERNAVN, FØDSELSDATO, KJØNN)
    }

    @Test
    fun `lagre infotrygdutbetalinger`() {
        dao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        assertEquals(1, infotrygdUtbetalinger().size)
    }

    @Test
    fun `oppretter person`() {
        val (personinfoId, enhetId, infotrygdutbetalingerId) = opprettPerson()
        assertNotNull(dao.findPersonByFødselsnummer(FNR.toLong()))
        assertNotNull(dao.findInfotrygdutbetalinger(FNR.toLong()))
        assertEquals(LocalDate.now(), dao.findEnhetSistOppdatert(FNR.toLong()))
        assertEquals(LocalDate.now(), dao.findITUtbetalingsperioderSistOppdatert(FNR.toLong()))
        assertEquals(LocalDate.now(), dao.findPersoninfoSistOppdatert(FNR.toLong()))
        assertEquals(1, person().size)
        person().first().assertEquals(FNR.toLong(), AKTØR.toLong(), personinfoId, enhetId, infotrygdutbetalingerId)
    }

    @Test
    fun `oppdaterer personinfo`() {
        opprettPerson()
        val nyttFornavn = "OLE"
        val nyttMellomnavn = "PETTER"
        val nyttEtternavn = "SVENSKE"
        val nyFødselsdato = LocalDate.of(1990, 12, 31)
        val nyttKjønn = Kjønn.Mann
        dao.updatePersoninfo(FNR.toLong(), nyttFornavn, nyttMellomnavn, nyttEtternavn, nyFødselsdato, nyttKjønn)
        assertPersoninfo(nyttFornavn, nyttMellomnavn, nyttEtternavn, nyFødselsdato, nyttKjønn)
    }

    @Test
    fun `oppdaterer enhet`() {
        opprettPerson()
        val nyEnhet = "2100".toInt()
        dao.updateEnhet(FNR.toLong(), nyEnhet)
        person().first().assertEnhet(nyEnhet)
    }

    private fun opprettPerson(): Triple<Int, Int, Int> {
        val personinfoId = dao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        val infotrygdutbetalingerId = dao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val enhetId = ENHET_OSLO.toInt()
        dao.insertPerson(FNR.toLong(), AKTØR.toLong(), personinfoId, enhetId, infotrygdutbetalingerId)
        return Triple(personinfoId, enhetId, infotrygdutbetalingerId)
    }

    private fun assertPersoninfo(forventetNavn: String, forventetMellomnavn: String?, forventetEtternavn: String?, forventetFødselsdato: LocalDate, forventetKjønn: Kjønn) {
        val personinfo = personinfo()
        assertEquals(1, personinfo.size)
        personinfo.first().assertEquals(forventetNavn, forventetMellomnavn, forventetEtternavn, forventetFødselsdato, forventetKjønn)
    }

    private fun infotrygdUtbetalinger() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT data FROM infotrygdutbetalinger").map { it.string("data") }.asList)
        }

    private fun person() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref FROM person").map {
                Person(
                    it.long("fodselsnummer"),
                    it.long("aktor_id"),
                    it.int("info_ref"),
                    it.int("enhet_ref"),
                    it.int("infotrygdutbetalinger_ref")
                )
            }.asList)
        }

    private fun personinfo() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT fornavn, mellomnavn, etternavn, fodselsdato, kjonn FROM person_info").map {
                Personinfo(
                    it.string("fornavn"),
                    it.stringOrNull("mellomnavn"),
                    it.string("etternavn"),
                    it.localDate("fodselsdato"),
                    enumValueOf(it.string("kjonn"))
                )
            }.asList)
        }

    private class Person(
        private val fødselsnummer: Long,
        private val aktørId: Long,
        private val infoRef: Int,
        private val enhetRef: Int,
        private val infotrygdutbetalingerRef: Int
    ) {
        fun assertEquals(forventetFødselsnummer: Long, forventetAktørId: Long, forventetInfoRef: Int, forventetEnhetRef: Int, forventetInfotrygdutbetalingerRef: Int) {
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
        private val kjønn: Kjønn
    ) {
        fun assertEquals(forventetNavn: String, forventetMellomnavn: String?, forventetEtternavn: String?, forventetFødselsdato: LocalDate, forventetKjønn: Kjønn) {
            assertEquals(forventetNavn, fornavn)
            assertEquals(forventetMellomnavn, mellomnavn)
            assertEquals(forventetEtternavn, etternavn)
            assertEquals(forventetFødselsdato, fødselsdato)
            assertEquals(forventetKjønn, kjønn)
        }
    }
}
