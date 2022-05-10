package no.nav.helse.e2e

import AbstractE2ETest
import io.mockk.every
import java.util.UUID
import org.junit.jupiter.api.Test

internal class AnnulleringE2ETest : AbstractE2ETest() {
    private val vedtaksperiodeId1: UUID = UUID.randomUUID()
    private val vedtaksperiodeId2: UUID = UUID.randomUUID()
    private val snapshotV1 = snapshot(1)
    private val snapshotV2 = snapshot(2)
    private val snapshotFinal = snapshot(3)

    @Test
    fun `utbetaling annullert oppdaterer alle snapshots på personen`() {
        val oid = UUID.randomUUID()
        val navn = "en saksbehandler"
        val epost = "saksbehandler_epost"
        val ident = "Z999999"
        saksbehandlerDao.opprettSaksbehandler(oid, navn, epost, ident)
        vedtaksperiode(vedtaksperiodeId = vedtaksperiodeId1, snapshot = snapshotV1, utbetalingId = UUID.randomUUID())
        vedtaksperiode(vedtaksperiodeId = vedtaksperiodeId2, snapshot = snapshotV2, utbetalingId = UUID.randomUUID())

        assertVedtak(vedtaksperiodeId2)
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotFinal
        sendUtbetalingAnnullert(saksbehandlerEpost = epost)

        assertSnapshot(snapshotFinal, vedtaksperiodeId1)
        assertSnapshot(snapshotFinal, vedtaksperiodeId2)
    }

}
