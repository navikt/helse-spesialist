package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.KomplettArbeidsforholdDto
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.apr
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class PgArbeidsforholdForTestDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()

    init {
        opprettArbeidsgiver()
    }

    private companion object {
        const val STILLINGSPROSENT = 100
        const val STILLINGSTITTEL = "Slabberasansvarlig"
        val STARTDATO: LocalDate = LocalDate.now()
        val SLUTTDATO: LocalDate? = null
    }

    @Test
    fun `oppretter arbeidsforhold`() {
        arbeidsforholdDao.upsertArbeidsforhold(person.id.value, ORGNUMMER, listOf(enKomplettArbeidsforholdDto()))
        arbeidsforholdDao.findArbeidsforhold(person.id.value, ORGNUMMER).first().also {
            assertEquals(STILLINGSPROSENT, it.stillingsprosent)
            assertEquals(STILLINGSTITTEL, it.stillingstittel)
            assertEquals(STARTDATO, it.startdato)
            assertEquals(SLUTTDATO, it.sluttdato)
        }
    }

    @Test
    fun `oppdaterer arbeidsforhold`() {
        val nyStartdato = LocalDate.now().minusDays(1)
        val nySluttdato = LocalDate.now()
        val nyStillingstittel = "Sto i med kona til sjefen"
        val nyStillingsprosent = 0
        val arbeidsforhold =
            listOf(
                Arbeidsforholdløsning.Løsning(nyStartdato, nySluttdato, nyStillingstittel, nyStillingsprosent),
            )
        arbeidsforholdDao.upsertArbeidsforhold(person.id.value, ORGNUMMER, listOf(enKomplettArbeidsforholdDto()))
        arbeidsforholdDao.upsertArbeidsforhold(
            person.id.value,
            ORGNUMMER,
            arbeidsforhold.map {
                KomplettArbeidsforholdDto(
                    fødselsnummer = person.id.value,
                    organisasjonsnummer = ORGNUMMER,
                    startdato = it.startdato,
                    sluttdato = it.sluttdato,
                    stillingstittel = it.stillingstittel,
                    stillingsprosent = it.stillingsprosent,
                )
            },
        )
        arbeidsforholdDao.findArbeidsforhold(person.id.value, ORGNUMMER).first().also {
            assertEquals(nyStillingsprosent, it.stillingsprosent)
            assertEquals(nyStillingstittel, it.stillingstittel)
            assertEquals(nyStartdato, it.startdato)
            assertEquals(nySluttdato, it.sluttdato)
        }
    }

    @Test
    fun `oppdaterer tomt arbeidsforhold`() {
        arbeidsforholdDao.upsertArbeidsforhold(person.id.value, ORGNUMMER, listOf(enKomplettArbeidsforholdDto()))
        arbeidsforholdDao.upsertArbeidsforhold(person.id.value, ORGNUMMER, emptyList())
        assertTrue(arbeidsforholdDao.findArbeidsforhold(person.id.value, ORGNUMMER).isEmpty())
    }

    @Test
    fun `finner arbeidsforhold`() {
        val fødselsnummer2 = opprettPerson().id.value
        val stillingstittel2 = "Slabberasmedarbeider"
        val fødselsnummer3 = opprettPerson().id.value
        val stillingstittel3 = "Stillingstittel3"

        arbeidsforholdDao.upsertArbeidsforhold(person.id.value, ORGNUMMER, listOf(enKomplettArbeidsforholdDto()))
        arbeidsforholdDao.upsertArbeidsforhold(
            fødselsnummer2,
            ORGNUMMER,
            listOf(enKomplettArbeidsforholdDto(fødselsnummer = fødselsnummer2, stillingstittel = stillingstittel2)),
        )
        arbeidsforholdDao.upsertArbeidsforhold(
            fødselsnummer3,
            ORGNUMMER,
            listOf(enKomplettArbeidsforholdDto(fødselsnummer = fødselsnummer3, stillingstittel = stillingstittel3)),
        )

        arbeidsforholdDao.findArbeidsforhold(fødselsnummer2, ORGNUMMER).first().also {
            assertEquals(stillingstittel2, it.stillingstittel)
        }

        arbeidsforholdDao.findArbeidsforhold(fødselsnummer3, ORGNUMMER).first().also {
            assertEquals(stillingstittel3, it.stillingstittel)
        }
    }

    @Test
    fun `finner sist oppdatert`() {
        arbeidsforholdDao.upsertArbeidsforhold(person.id.value, ORGNUMMER, listOf(enKomplettArbeidsforholdDto()))
        arbeidsforholdDao.findArbeidsforhold(person.id.value, ORGNUMMER).first().also {
            assertTrue(it.oppdatert > LocalDateTime.now().minusDays(10))
        }
    }

    @Test
    fun `skriver ikke over alle arbeidsforhold med samme informasjon`() {
        val løsninger =
            Arbeidsforholdløsning(
                listOf(
                    Arbeidsforholdløsning.Løsning(1 jan 2018, null, "Nerd", 50),
                    Arbeidsforholdløsning.Løsning(20 apr 2018, null, "Noe annet", 50),
                ),
            )

        val now = LocalDateTime.now()
        løsninger.upsert(arbeidsforholdDao, person.id.value, ORGNUMMER, now)
        val arbeidsforhold = arbeidsforholdDao.findArbeidsforhold(person.id.value, ORGNUMMER)

        løsninger.upsert(arbeidsforholdDao, person.id.value, ORGNUMMER, now)
        assertEquals(arbeidsforhold, arbeidsforholdDao.findArbeidsforhold(person.id.value, ORGNUMMER))
    }

    @Test
    fun `fjerner arbeidsforhold som er borte fra aareg`() {
        Arbeidsforholdløsning(
            listOf(
                Arbeidsforholdløsning.Løsning(1 jan 2018, null, "Nerd", 50),
                Arbeidsforholdløsning.Løsning(20 apr 2018, null, "Noe annet", 50),
            ),
        ).upsert(arbeidsforholdDao, person.id.value, ORGNUMMER)

        Arbeidsforholdløsning(
            listOf(
                Arbeidsforholdløsning.Løsning(1 jan 2018, null, "Nerd", 50),
            ),
        ).upsert(arbeidsforholdDao, person.id.value, ORGNUMMER)

        assertEquals(1, arbeidsforholdDao.findArbeidsforhold(person.id.value, ORGNUMMER).size)
    }

    private fun enKomplettArbeidsforholdDto(
        fødselsnummer: String = person.id.value,
        organisasjonsnummer: String = ORGNUMMER,
        startdato: LocalDate = STARTDATO,
        sluttdato: LocalDate? = SLUTTDATO,
        stillingstittel: String = STILLINGSTITTEL,
        stillingsprosent: Int = STILLINGSPROSENT,
        oppdatert: LocalDateTime = LocalDateTime.now(),
    ) = KomplettArbeidsforholdDto(
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        startdato = startdato,
        sluttdato = sluttdato,
        stillingstittel = stillingstittel,
        stillingsprosent = stillingsprosent,
        oppdatert = oppdatert,
    )
}
