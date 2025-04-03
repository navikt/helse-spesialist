package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class VedtakFattetE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `når saksbehandler fatter vedtak får behandlingen tilstand VedtakFattet`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        // When:
        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak()
        }

        // Then:
        assertBehandlingTilstand("VedtakFattet")
        assertVedtakFattetEtterHovedregel()
    }
}
