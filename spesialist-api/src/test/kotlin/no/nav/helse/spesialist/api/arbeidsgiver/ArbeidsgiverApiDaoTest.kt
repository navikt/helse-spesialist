package no.nav.helse.spesialist.api.arbeidsgiver

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsgiverApiDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner bransjer`() {
        val bransjer = listOf("bransje 1", "bransje 2")
        opprettArbeidsgiver(bransjer = bransjer)
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
        val personId = opprettPerson()
        val arbeidsgiverId = opprettArbeidsgiver()
        opprettVedtaksperiode(personId, arbeidsgiverId)
        opprettArbeidsforhold(personId, arbeidsgiverId)

        val arbeidsforhold = arbeidsgiverApiDao.finnArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER)

        assertEquals(1, arbeidsforhold.size)
        assertEquals(ARBEIDSFORHOLD.start, arbeidsforhold.first().startdato)
        assertEquals(ARBEIDSFORHOLD.slutt, arbeidsforhold.first().sluttdato)
        assertEquals(ARBEIDSFORHOLD.tittel, arbeidsforhold.first().stillingstittel)
        assertEquals(ARBEIDSFORHOLD.prosent, arbeidsforhold.first().stillingsprosent)
    }
}
