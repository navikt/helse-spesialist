package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import org.junit.jupiter.api.Test

internal class AnnulleringE2ETest : AbstractE2ETest() {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)

    @Test
    fun `annullert utbetaling medfører at snapshot blir oppdatert`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        assertNyttSnapshot {
            håndterUtbetalingAnnullert(saksbehandler_epost = SAKSBEHANDLER_EPOST)
        }
    }
}
