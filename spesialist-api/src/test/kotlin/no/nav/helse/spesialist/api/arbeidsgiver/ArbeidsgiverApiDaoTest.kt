package no.nav.helse.spesialist.api.arbeidsgiver

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsgiverApiDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner bransjer`() {
        val bransjer = listOf("bransje 1", "bransje 2")
        opprettArbeidsgiver(bransjer)
        assertEquals(bransjer, arbeidsgiverApiDao.finnBransjer(ORGANISASJONSNUMMER))
    }

    @Test
    fun `finner bransjer når det ikke er noen bransjer`() {
        opprettArbeidsgiver()
        assertTrue(arbeidsgiverApiDao.finnBransjer(ORGANISASJONSNUMMER).isEmpty())
    }

    @Test
    fun `finner navn`() {
        opprettArbeidsgiver()
        assertEquals(ARBEIDSGIVER_NAVN, arbeidsgiverApiDao.finnNavn(ORGANISASJONSNUMMER))
    }

    @Test
    fun `finner arbeidsforhold`() {
        val (startdato, sluttdato, tittel, prosent) = ARBEIDSFORHOLD
        opprettVedtaksperiode()
        val arbeidsforhold = arbeidsgiverApiDao.finnArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER)
        assertEquals(1, arbeidsforhold.size)
        assertEquals(startdato, arbeidsforhold.first().startdato)
        assertEquals(sluttdato, arbeidsforhold.first().sluttdato)
        assertEquals(tittel, arbeidsforhold.first().stillingstittel)
        assertEquals(prosent, arbeidsforhold.first().stillingsprosent)
    }
}
