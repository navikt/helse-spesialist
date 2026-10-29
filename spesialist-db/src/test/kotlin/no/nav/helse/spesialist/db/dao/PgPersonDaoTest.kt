package no.nav.helse.spesialist.db.dao

import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.YearMonth

@Isolated
internal class PgPersonDaoTest : AbstractDBIntegrationTest() {
    @BeforeEach
    fun tømTabeller() {
        dbQuery.execute("truncate person_info, inntekt restart identity cascade")
    }

    @Test
    fun `finner adressebeskyttelse for person`() {
        val person = opprettPerson(person = lagPerson(adressebeskyttelse = no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse.Fortrolig))
        assertEquals(Adressebeskyttelse.Fortrolig, personDao.finnAdressebeskyttelse(person.id.value))
    }

    @Test
    fun `Lagrer inntekt`() {
        val person = opprettPerson()
        val sekvensnummer =
            personDao.lagreInntekter(
                fødselsnummer = person.id.value,
                skjæringstidspunkt = LocalDate.parse("2022-11-11"),
                inntekter =
                    listOf(
                        Inntekter(
                            YearMonth.parse("2022-11"),
                            listOf(Inntekter.Inntekt(20000.0, ORGNUMMER)),
                        ),
                        Inntekter(
                            YearMonth.parse("2022-10"),
                            listOf(Inntekter.Inntekt(20000.0, ORGNUMMER)),
                        ),
                        Inntekter(
                            YearMonth.parse("2022-09"),
                            listOf(Inntekter.Inntekt(20000.0, ORGNUMMER)),
                        ),
                    ),
            )
        assertEquals(1, sekvensnummer)

        val inntekter = inntekter()
        assertEquals(3, inntekter.size)
        assertEquals(YearMonth.parse("2022-11"), inntekter.first().årMåned)
    }

    @Test
    fun `Finner riktig inntekt`() {
        val person = opprettPerson()
        personDao.lagreInntekter(
            fødselsnummer = person.id.value,
            skjæringstidspunkt = LocalDate.parse("2022-11-11"),
            inntekter = listOf(Inntekter(YearMonth.parse("2022-11"), listOf(Inntekter.Inntekt(20000.0, ORGNUMMER)))),
        )
        personDao.lagreInntekter(
            fødselsnummer = person.id.value,
            skjæringstidspunkt = LocalDate.parse("2022-03-03"),
            inntekter = listOf(Inntekter(YearMonth.parse("2022-03"), listOf(Inntekter.Inntekt(20000.0, ORGNUMMER)))),
        )
        personDao.lagreInntekter(
            fødselsnummer = person.id.value,
            skjæringstidspunkt = LocalDate.parse("2020-01-01"),
            inntekter = listOf(Inntekter(YearMonth.parse("2021-01"), listOf(Inntekter.Inntekt(20000.0, ORGNUMMER)))),
        )

        assertEquals(
            YearMonth.parse("2022-11"),
            personDao.finnInntekter(person.id.value, LocalDate.parse("2022-11-11"))!!.first().årMåned,
        )
        assertEquals(
            YearMonth.parse("2022-03"),
            personDao.finnInntekter(person.id.value, LocalDate.parse("2022-03-03"))!!.first().årMåned,
        )
        assertEquals(
            YearMonth.parse("2021-01"),
            personDao.finnInntekter(person.id.value, LocalDate.parse("2020-01-01"))!!.first().årMåned,
        )
    }

    @Test
    fun `finner alle inntekter fra a-ordningen for personen`() {
        val person = opprettPerson()
        val annenPerson = opprettPerson()
        val novemberInntekter = listOf(Inntekter(YearMonth.parse("2022-11"), listOf(Inntekter.Inntekt(20000.0, ORGNUMMER))))
        val marsInntekter = listOf(Inntekter(YearMonth.parse("2022-03"), listOf(Inntekter.Inntekt(10000.0, ORGNUMMER))))
        personDao.lagreInntekter(person.id.value, LocalDate.parse("2022-11-11"), novemberInntekter)
        personDao.lagreInntekter(person.id.value, LocalDate.parse("2022-03-03"), marsInntekter)
        personDao.lagreInntekter(annenPerson.id.value, LocalDate.parse("2022-11-11"), novemberInntekter)

        assertEquals(
            mapOf(
                LocalDate.parse("2022-03-03") to marsInntekter,
                LocalDate.parse("2022-11-11") to novemberInntekter,
            ),
            personDao.finnInntekterFraAordningen(person.id.value),
        )
    }

    @Test
    fun `finner ingen inntekter fra a-ordningen for person uten inntekter`() {
        assertEquals(emptyMap<LocalDate, List<Inntekter>>(), personDao.finnInntekterFraAordningen(opprettPerson().id.value))
    }

    private fun inntekter(): List<Inntekter> =
        dbQuery.singleOrNull("SELECT inntekter from inntekt") { row ->
            objectMapper.readValue<List<Inntekter>>(row.string("inntekter"))
        } ?: emptyList()
}
