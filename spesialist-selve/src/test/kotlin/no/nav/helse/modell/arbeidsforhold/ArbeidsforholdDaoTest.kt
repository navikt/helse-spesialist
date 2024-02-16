package no.nav.helse.modell.arbeidsforhold

import DatabaseIntegrationTest
import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ArbeidsforholdDaoTest : DatabaseIntegrationTest() {
    private companion object {
        const val STILLINGSPROSENT = 100
        const val STILLINGSTITTEL = "Slabberasansvarlig"
        val STARTDATO: LocalDate = LocalDate.now()
        val SLUTTDATO: LocalDate? = null
    }

    @BeforeEach
    fun setup() {
        opprettPerson()
        opprettArbeidsgiver()
    }

    @Test
    fun `oppretter arbeidsforhold`() {
        arbeidsforholdDao.insertArbeidsforhold(FNR, ORGNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER).first().also {
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
        val arbeidsforhold = listOf(
            Arbeidsforholdløsning.Løsning(nyStartdato, nySluttdato, nyStillingstittel, nyStillingsprosent)
        )
        arbeidsforholdDao.insertArbeidsforhold(FNR, ORGNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.oppdaterArbeidsforhold(FNR, ORGNUMMER, arbeidsforhold)
        arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER).first().also {
            assertEquals(nyStillingsprosent, it.stillingsprosent)
            assertEquals(nyStillingstittel, it.stillingstittel)
            assertEquals(nyStartdato, it.startdato)
            assertEquals(nySluttdato, it.sluttdato)
        }
    }

    @Test
    fun `oppdaterer tomt arbeidsforhold`() {
        arbeidsforholdDao.insertArbeidsforhold(FNR, ORGNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.oppdaterArbeidsforhold(FNR, ORGNUMMER, emptyList())
        assertTrue(arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER).isEmpty())
    }

    @Test
    fun `finner arbeidsforhold`() {
        val fødselsnummer2 = "10273645893"
        val stillingstittel2 = "Slabberasmedarbeider"
        val fødselsnummer3 = "10273645894"
        val stillingstittel3 = "Stillingstittel3"
        opprettPerson(fødselsnummer2)
        opprettPerson(fødselsnummer3)

        arbeidsforholdDao.insertArbeidsforhold(FNR, ORGNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.insertArbeidsforhold(
            fødselsnummer2,
            ORGNUMMER,
            STARTDATO,
            SLUTTDATO,
            stillingstittel2,
            STILLINGSPROSENT
        )
        arbeidsforholdDao.insertArbeidsforhold(
            fødselsnummer3,
            ORGNUMMER,
            STARTDATO,
            SLUTTDATO,
            stillingstittel3,
            STILLINGSPROSENT
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
        arbeidsforholdDao.insertArbeidsforhold(FNR, ORGNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.findArbeidsforholdSistOppdatert(FNR, ORGNUMMER).also {
            assertTrue(it!! > LocalDate.now().minusDays(10))
        }
    }

    @Test
    fun `skriver ikke over alle arbeidsforhold med samme informasjon`() {
        val løsninger = Arbeidsforholdløsning(
            listOf(
                Arbeidsforholdløsning.Løsning(1.januar, null, "Nerd", 50),
                Arbeidsforholdløsning.Løsning(20.april, null, "Noe annet", 50)
            )
        )

        løsninger.opprett(arbeidsforholdDao, FNR, ORGNUMMER)
        val arbeidsforhold = arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER)

        løsninger.oppdater(arbeidsforholdDao, FNR, ORGNUMMER)
        assertEquals(arbeidsforhold, arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER))
    }

    @Test
    fun `fjerner arbeidsforhold som er borte fra aareg`() {
        Arbeidsforholdløsning(
            listOf(
                Arbeidsforholdløsning.Løsning(1.januar, null, "Nerd", 50),
                Arbeidsforholdløsning.Løsning(20.april, null, "Noe annet", 50)
            )
        ).opprett(arbeidsforholdDao, FNR, ORGNUMMER)

        Arbeidsforholdløsning(
            listOf(
                Arbeidsforholdløsning.Løsning(1.januar, null, "Nerd", 50)
            )
        ).oppdater(arbeidsforholdDao, FNR, ORGNUMMER)

        assertEquals(1, arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER).size)
    }
}
