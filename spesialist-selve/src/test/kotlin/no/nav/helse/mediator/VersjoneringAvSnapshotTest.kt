package no.nav.helse.mediator

import AbstractE2ETest
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import java.util.UUID
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.snapshot
import org.junit.jupiter.api.Test

internal class VersjoneringAvSnapshotTest : AbstractE2ETest() {

    @Test
    fun `utdatert snapshot`() {
        val utbetalingId = UUID.randomUUID()
        val gammelSnapshot = snapshot(-1, utbetalingId = utbetalingId)
        val nyttSnapshot = snapshot(utbetalingId = utbetalingId)
        vedtaksperiode(utbetalingId = utbetalingId, snapshot = gammelSnapshot)

        clearMocks(snapshotClient)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns nyttSnapshot
        snapshotMediator.hentSnapshot(FØDSELSNUMMER)
        verify(exactly = 1) { snapshotClient.hentSnapshot(FØDSELSNUMMER) }

        clearMocks(snapshotClient)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns nyttSnapshot
        snapshotMediator.hentSnapshot(FØDSELSNUMMER)
        verify(exactly = 0) { snapshotClient.hentSnapshot(FØDSELSNUMMER) }
    }

}
