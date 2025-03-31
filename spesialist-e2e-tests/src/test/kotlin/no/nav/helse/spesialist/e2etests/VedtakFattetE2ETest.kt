package no.nav.helse.spesialist.e2etests

import org.junit.jupiter.api.Test

class VedtakFattetE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `når saksbehandler fatter vedtak får behandlingen tilstand VedtakFattet`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        simulerFremTilOgMedGodkjenningsbehov()
        saksbehandlerTildelerSegSaken()

        // When:
        saksbehandlerGodkjennerAlleVarsler()
        saksbehandlerFatterVedtak()

        // Then:
        assertBehandlingTilstand("VedtakFattet")
    }
}
