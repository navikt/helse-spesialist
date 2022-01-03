package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.snapshotMedWarning
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class OppdaterSnapshotCommandTest {

    private companion object {
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private const val FNR = "fnr"
        private val SNAPSHOT = snapshotMedWarning(VEDTAKSPERIODE, "", "", "")
        private const val VEDTAK_REF = 1L
    }

    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val context = CommandContext(UUID.randomUUID())

    private val command = OppdaterSnapshotCommand(restClient, vedtakDao, warningDao, snapshotDao, VEDTAKSPERIODE, FNR)

    @BeforeEach
    fun setup() {
        clearMocks(vedtakDao, snapshotDao, restClient)
    }

    @Test
    fun `ignorerer vedtaksperioder som ikke finnes`() {
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE) } returns null
        assertTrue(command.execute(context))
        verify(exactly = 0) { restClient.hentSpeilSpapshot(FNR) }
        verify(exactly = 0) { snapshotDao.lagre(FNR, any()) }
    }

    @Test
    fun `lagrer snapshot`() {
        test { assertTrue(command.execute(context)) }
        verify(exactly = 1) { warningDao.oppdaterSpleisWarnings(VEDTAKSPERIODE, any()) }
    }

    private fun test(block: () -> Unit) {
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE) } returns VEDTAK_REF
        every { restClient.hentSpeilSpapshot(FNR) } returns SNAPSHOT
        every { snapshotDao.lagre(FNR, SNAPSHOT) } returns 1
        block()
        verify(exactly = 1) { restClient.hentSpeilSpapshot(FNR) }
        verify(exactly = 1) { snapshotDao.lagre(FNR, SNAPSHOT) }
    }
}
