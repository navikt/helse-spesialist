package no.nav.helse.arbeidsgiver

import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsgiverApiDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner bransjer`() {
        val bransjer = listOf("bransje 1", "bransje 2")
        arbeidsgiver(bransjer)
        assertEquals(bransjer, arbeidsgiverApiDao.finnBransjer(ORGANISASJONSNUMMER))
    }

    @Test
    fun `finner bransjer når det ikke er noen bransjer`() {
        arbeidsgiver()
        assertTrue(arbeidsgiverApiDao.finnBransjer(ORGANISASJONSNUMMER).isEmpty())
    }

    @Test
    fun `finner navn`() {
        arbeidsgiver()
        assertEquals(ARBEIDSGIVER_NAVN, arbeidsgiverApiDao.finnNavn(ORGANISASJONSNUMMER))
    }

    @Test
    fun `finner arbeidsforhold`() {
        val (startdato, sluttdato, tittel, prosent) = ARBEIDSFORHOLD
        nyVedtaksperiode()
        val arbeidsforhold = arbeidsgiverApiDao.finnArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER)
        assertEquals(1, arbeidsforhold.size)
        assertEquals(startdato, arbeidsforhold.first().startdato)
        assertEquals(sluttdato, arbeidsforhold.first().sluttdato)
        assertEquals(tittel, arbeidsforhold.first().stillingstittel)
        assertEquals(prosent, arbeidsforhold.first().stillingsprosent)
    }
}
