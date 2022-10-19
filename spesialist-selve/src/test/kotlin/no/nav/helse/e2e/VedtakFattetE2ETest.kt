package no.nav.helse.e2e

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.Testdata.FØDSELSNUMMER
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class VedtakFattetE2ETest: AbstractE2ETest() {

    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    @Test
    fun `vedtak fattet medfører låsing av vedtaksperiode-generasjon`() {
        settOppBruker()
        assertDoesNotThrow {
            sendVedtakFattet(FØDSELSNUMMER, VEDTAKSPERIODE_ID)
        }
    }
}
