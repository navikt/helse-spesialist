package no.nav.helse.spesialist.e2etests

import org.junit.jupiter.api.Test

class VedtakFattetE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `vedtak fattet medfører låsing av vedtaksperiode-generasjon`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        simulerFremTilOgMedGodkjenningsbehov()
        // Saksbehandler vurderer varsel
        // Saksbehandler trykker fatt vedtak-knappen
        håndterUtbetalingUtbetalt()
        håndterAvsluttetMedVedtak()
        assertBehandlingTilstand("VedtakFattet")
    }
}
