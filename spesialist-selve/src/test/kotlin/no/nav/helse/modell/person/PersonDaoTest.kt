package no.nav.helse.modell.person

import DatabaseIntegrationTest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.YearMonth
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
internal class PersonDaoTest : DatabaseIntegrationTest() {

    @BeforeEach
    fun tømTabeller() {
        sessionOf(dataSource).use  {
            it.run(queryOf("truncate person_info, inntekt, infotrygdutbetalinger restart identity cascade").asExecute)
        }
    }

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
    fun `finn personDto`() {
        opprettPerson()
        val personDto = with(personDao) {
            sessionOf(dataSource).transaction {
                it.finnPerson(FNR)
            }
        }
        assertNotNull(personDto)
        assertEquals(FNR, personDto?.fødselsnummer)
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
    fun `oppretter person klar for visning i støttetabell for skjønnsmessig fastsettelse`() {
        opprettMinimalPerson()
        personDao.markerPersonSomKlarForVisning(FNR)

        val personKlarForVisning = sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "SELECT fodselsnummer FROM stottetabell_for_skjonnsmessig_fastsettelse WHERE fodselsnummer = ?",
                    FNR.toLong()
                ).map { it.long("fodselsnummer").toFødselsnummer() }.asSingle
            )
        }
        assertEquals(FNR, personKlarForVisning)
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
    fun `finner aktørId`() {
        opprettPerson()
        assertEquals(AKTØR, personDao.finnAktørId(FNR))
    }

    @Test
    fun `finner ikke aktørId om vi ikke har person`() {
        assertEquals(null, personDao.finnAktørId(FNR))
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

    @Test
    fun `Lagrer inntekt`() {
        opprettPerson()
        val sekvensnummer = personDao.insertInntekter(
            fødselsnummer = FNR,
            skjæringstidspunkt = LocalDate.parse("2022-11-11"),
            inntekter = listOf(
                Inntekter(
                    YearMonth.parse("2022-11"),
                    listOf(Inntekter.Inntekt(20000.0, ORGNUMMER))
                ),
                Inntekter(
                    YearMonth.parse("2022-10"),
                    listOf(Inntekter.Inntekt(20000.0, ORGNUMMER))
                ),
                Inntekter(
                    YearMonth.parse("2022-09"),
                    listOf(Inntekter.Inntekt(20000.0, ORGNUMMER))
                )
            )
        )
        assertEquals(1, sekvensnummer)

        val inntekter = inntekter()
        assertEquals(3, inntekter.size)
        assertEquals(YearMonth.parse("2022-11"), inntekter.first().årMåned)
    }

    @Test
    fun `Finner riktig inntekt`() {
        opprettPerson()
        personDao.insertInntekter(
            fødselsnummer = FNR,
            skjæringstidspunkt = LocalDate.parse("2022-11-11"),
            inntekter = listOf(Inntekter(YearMonth.parse("2022-11"), listOf(Inntekter.Inntekt(20000.0, ORGNUMMER))))
        )
        personDao.insertInntekter(
            fødselsnummer = FNR,
            skjæringstidspunkt = LocalDate.parse("2022-03-03"),
            inntekter = listOf(Inntekter(YearMonth.parse("2022-03"), listOf(Inntekter.Inntekt(20000.0, ORGNUMMER))))
        )
        personDao.insertInntekter(
            fødselsnummer = FNR,
            skjæringstidspunkt = LocalDate.parse("2020-01-01"),
            inntekter = listOf(Inntekter(YearMonth.parse("2021-01"), listOf(Inntekter.Inntekt(20000.0, ORGNUMMER))))
        )

        assertEquals(YearMonth.parse("2022-11"), personDao.findInntekter(FNR, LocalDate.parse("2022-11-11"))!!.first().årMåned)
        assertEquals(YearMonth.parse("2022-03"), personDao.findInntekter(FNR, LocalDate.parse("2022-03-03"))!!.first().årMåned)
        assertEquals(YearMonth.parse("2021-01"), personDao.findInntekter(FNR, LocalDate.parse("2020-01-01"))!!.first().årMåned)
    }

    @Test
    fun `slår opp person som har passert filter`() {
        val ingenPersoner = personDao.findPersonerSomHarPassertFilter()
        assertTrue(ingenPersoner.isEmpty())
        opprettPersonSomHarPassertFilter("12345678910")
        opprettPersonSomHarPassertFilter("02345678911")
        val personer = personDao.findPersonerSomHarPassertFilter()
        assertEquals(2, personer.size)
        assertTrue(personer.contains(2345678911L))
    }

    private fun opprettPersonSomHarPassertFilter(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query =
            """
                INSERT INTO passert_filter_for_skjonnsfastsettelse(fodselsnummer) 
                VALUES (?)
            """
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer.toLong()).asExecute)
        }
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

    private fun inntekter(): List<Inntekter> = sessionOf(dataSource).use { session ->
        session.run(
            queryOf("SELECT inntekter from inntekt")
                .map { row ->
                    objectMapper.readValue<List<Inntekter>>(row.string("inntekter"))
                }
                .asSingle
        ) ?: emptyList()
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
