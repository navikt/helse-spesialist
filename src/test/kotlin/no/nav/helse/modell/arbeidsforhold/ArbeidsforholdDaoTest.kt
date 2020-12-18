package no.nav.helse.modell.arbeidsforhold

import DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsforholdDaoTest: DatabaseIntegrationTest() {
    private companion object {
        const val FØDSELSNUMMER = "12345678910"
        const val ORGANISASJONSNUMMER = "987654321"
        const val STILLINGSPROSENT = 100
        const val STILLINGSTITTEL = "Slabberasansvarlig"
        val STARTDATO: LocalDate = LocalDate.now()
        val SLUTTDATO = null
    }

    @Test
    fun `oppretter arbeidsforhold`() {
        opprettPerson(FØDSELSNUMMER)
        opprettArbeidsgiver(ORGANISASJONSNUMMER)
        arbeidsforholdDao.insertArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.findArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER).first().also {
            assertEquals(STILLINGSPROSENT, it?.stillingsprosent)
            assertEquals(STILLINGSTITTEL, it?.stillingstittel)
            assertEquals(STARTDATO, it?.startdato)
            assertEquals(SLUTTDATO, it?.sluttdato)
        }
    }

    @Test
    fun `oppdaterer arbeidsforhold`() {
        val nyStartdato = LocalDate.now().minusDays(1)
        val nySluttdato = LocalDate.now()
        val nyStillingstittel = "Sto i med kona til sjefen"
        val nyStillingsprosent = 0
        opprettPerson(FØDSELSNUMMER)
        opprettArbeidsgiver(ORGANISASJONSNUMMER)
        arbeidsforholdDao.insertArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.oppdaterArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER, nyStartdato, nySluttdato, nyStillingstittel, nyStillingsprosent)
        arbeidsforholdDao.findArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER).first().also {
            assertEquals(nyStillingsprosent, it?.stillingsprosent)
            assertEquals(nyStillingstittel, it?.stillingstittel)
            assertEquals(nyStartdato, it?.startdato)
            assertEquals(nySluttdato, it?.sluttdato)
        }
    }

    @Test
    fun `finner arbeidsforhold`() {
        val fødselsnummer2 = "10273645893"
        val stillingstittel2 = "Slabberasmedarbeider"
        val fødselsnummer3 = "10273645894"
        val stillingstittel3 = "Stillingstittel3"
        opprettPerson(FØDSELSNUMMER)
        opprettPerson(fødselsnummer2)
        opprettPerson(fødselsnummer3)
        opprettArbeidsgiver(ORGANISASJONSNUMMER)

        arbeidsforholdDao.insertArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.insertArbeidsforhold(fødselsnummer2, ORGANISASJONSNUMMER, STARTDATO, SLUTTDATO, stillingstittel2, STILLINGSPROSENT)
        arbeidsforholdDao.insertArbeidsforhold(fødselsnummer3, ORGANISASJONSNUMMER, STARTDATO, SLUTTDATO, stillingstittel3, STILLINGSPROSENT)

        arbeidsforholdDao.findArbeidsforhold(fødselsnummer2, ORGANISASJONSNUMMER).first().also {
            assertEquals(stillingstittel2, it?.stillingstittel)
        }

        arbeidsforholdDao.findArbeidsforhold(fødselsnummer3, ORGANISASJONSNUMMER).first().also {
            assertEquals(stillingstittel3, it?.stillingstittel)
        }
    }

    @Test
    fun `finner sist oppdatert`() {
        opprettPerson(FØDSELSNUMMER)
        opprettArbeidsgiver(ORGANISASJONSNUMMER)
        arbeidsforholdDao.insertArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER, STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforholdDao.findArbeidsforholdSistOppdatert(FØDSELSNUMMER, ORGANISASJONSNUMMER).also {
            assertTrue(it > LocalDate.now().minusDays(10))
        }
    }
}
