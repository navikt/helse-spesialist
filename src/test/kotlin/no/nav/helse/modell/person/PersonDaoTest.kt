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
        assertNotNull(dao.findPersonByFødselsnummer(FNR))
        assertNotNull(dao.findInfotrygdutbetalinger(FNR))
        assertEquals(LocalDate.now(), dao.findEnhetSistOppdatert(FNR))
        assertEquals(LocalDate.now(), dao.findITUtbetalingsperioderSistOppdatert(FNR))
        assertEquals(LocalDate.now(), dao.findPersoninfoSistOppdatert(FNR))
        assertEquals(1, person().size)
        person().first().assertEquals(FNR, AKTØR, personinfoId, enhetId, infotrygdutbetalingerId)
    }

    @Test
    fun `oppdaterer personinfo`() {
        opprettPerson()
        val nyttFornavn = "OLE"
        val nyttMellomnavn = "PETTER"
        val nyttEtternavn = "SVENSKE"
        val nyFødselsdato = LocalDate.of(1990, 12, 31)
        val nyttKjønn = Kjønn.Mann
        dao.updatePersoninfo(FNR, nyttFornavn, nyttMellomnavn, nyttEtternavn, nyFødselsdato, nyttKjønn)
        assertPersoninfo(nyttFornavn, nyttMellomnavn, nyttEtternavn, nyFødselsdato, nyttKjønn)
    }

    @Test
    fun `oppdaterer enhet`() {
        opprettPerson()
        val nyEnhet = "2100".toInt()
        dao.updateEnhet(FNR, nyEnhet)
        person().first().assertEnhet(nyEnhet)
    }

    @Test
    fun `oppdaterer infotrygdutbetalingerRef`() {
        opprettPerson()
        dao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val infotrygdUtbetalingerRef = "2".toInt()
        dao.updateInfotrygdutbetalingerRef(FNR, infotrygdUtbetalingerRef)
        person().first().assertInfotrygdUtbetalingerRef(infotrygdUtbetalingerRef)
    }

    private fun opprettPerson(): Triple<Int, Int, Int> {
        val personinfoId = dao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        val infotrygdutbetalingerId = dao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val enhetId = ENHET_OSLO.toInt()
        dao.insertPerson(FNR, AKTØR, personinfoId, enhetId, infotrygdutbetalingerId)
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
                    it.long("fodselsnummer").toFødselsnummer(),
                    it.long("aktor_id").toString(),
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
        private val fødselsnummer: String,
        private val aktørId: String,
        private val infoRef: Int,
        private val enhetRef: Int,
        private val infotrygdutbetalingerRef: Int
    ) {
        fun assertEquals(forventetFødselsnummer: String, forventetAktørId: String, forventetInfoRef: Int, forventetEnhetRef: Int, forventetInfotrygdutbetalingerRef: Int) {
            assertEquals(forventetFødselsnummer, fødselsnummer)
            assertEquals(forventetAktørId, aktørId)
            assertEquals(forventetInfoRef, infoRef)
            assertEquals(forventetEnhetRef, enhetRef)
            assertEquals(forventetInfotrygdutbetalingerRef, infotrygdutbetalingerRef)
        }

        fun assertEnhet(forventetEnhet: Int) {
            assertEquals(forventetEnhet, enhetRef)
        }
        fun assertInfotrygdUtbetalingerRef(forventetInfotrygdutbetalingerRef: Int) {
            assertEquals(forventetInfotrygdutbetalingerRef, infotrygdutbetalingerRef)
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
