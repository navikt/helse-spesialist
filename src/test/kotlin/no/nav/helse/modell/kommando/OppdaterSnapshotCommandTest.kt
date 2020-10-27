package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.snapshot
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class OppdaterSnapshotCommandTest {

    private companion object {
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private const val FNR = "fnr"
        private val SNAPSHOT = snapshot(VEDTAKSPERIODE)
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
        verify(exactly = 0) { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE, any()) }
    }

    @Test
    fun `lagrer snapshot`() {
        okTest { assertTrue(command.execute(context)) }
        verify(exactly = 1) { warningDao.oppdaterSpleisWarnings(VEDTAKSPERIODE, any()) }
    }

    @Test
    fun `resume henter snapshot`() {
        okTest { assertTrue(command.resume(context)) }
    }

    @Test
    fun `resume oppdaterer warnings`() {
        val forventetWarning = Warning(
            melding = "Brukeren har flere inntekter de siste tre måneder.",
            kilde = WarningKilde.Spleis
        )
        okTest { assertTrue(command.resume(context)) }
        verify(exactly = 1) { warningDao.oppdaterSpleisWarnings(VEDTAKSPERIODE, listOf(forventetWarning)) }
    }

    @Test
    fun `resume suspender ved feil`() {
        failingTest { assertFalse(command.resume(context)) }
    }

    @Test
    fun `suspender ved feil`() {
        failingTest { assertFalse(command.execute(context)) }
    }

    private fun okTest(block: () -> Unit) {
        test(true, block)
    }

    private fun failingTest(block: () -> Unit) {
        test(false, block)
    }

    private fun test(ok: Boolean, block: () -> Unit) {
        every { vedtakDao.finnVedtakId(VEDTAKSPERIODE) } returns VEDTAK_REF
        every { restClient.hentSpeilSpapshot(FNR) } returns SNAPSHOT
        every { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE, SNAPSHOT) } returns if (ok) 1 else 0
        block()
        verify(exactly = 1) { restClient.hentSpeilSpapshot(FNR) }
        verify(exactly = 1) { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE, SNAPSHOT) }
    }
}
