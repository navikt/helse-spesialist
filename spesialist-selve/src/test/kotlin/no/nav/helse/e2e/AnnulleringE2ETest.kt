package no.nav.helse.e2e

import AbstractE2ETestV2
import no.nav.helse.Testdata.SAKSBEHANDLER_EPOST
import no.nav.helse.Testdata.SAKSBEHANDLER_IDENT
import no.nav.helse.Testdata.SAKSBEHANDLER_NAVN
import no.nav.helse.Testdata.SAKSBEHANDLER_OID
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import org.junit.jupiter.api.Test

internal class AnnulleringE2ETest : AbstractE2ETestV2() {
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)

    @Test
    fun `annullert utbetaling medfører at snapshot blir oppdatert`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        assertNyttSnapshot {
            håndterUtbetalingAnnullert()
        }
    }
}
