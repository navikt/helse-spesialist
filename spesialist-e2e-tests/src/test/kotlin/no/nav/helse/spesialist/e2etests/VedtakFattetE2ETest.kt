package no.nav.helse.spesialist.e2etests

import org.junit.jupiter.api.Test

class VedtakFattetE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `vedtak fattet medfører låsing av vedtaksperiode-generasjon`() {
        // Given:
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        simulerFremTilOgMedGodkjenningsbehov()
        saksbehandlerTildelerSegSaken()

        // When:
        saksbehandlerGodkjennerRisikovurderingVarsel()
        saksbehandlerFatterVedtak()

        // Then:
        assertBehandlingTilstand("VedtakFattet")
    }
}
