package no.nav.helse.e2e

import AbstractE2ETest
import org.junit.jupiter.api.Test

internal class AnnulleringE2ETest : AbstractE2ETest() {

    @Test
    fun `annullert utbetaling medfører at snapshot blir oppdatert`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        assertNyttSnapshot {
            håndterUtbetalingAnnullert(saksbehandler_epost = SAKSBEHANDLER_EPOST)
        }
    }
}
