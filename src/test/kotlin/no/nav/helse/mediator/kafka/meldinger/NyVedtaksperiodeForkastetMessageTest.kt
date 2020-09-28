package no.nav.helse.mediator.kafka.meldinger

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.kafka.Hendelsefabrikk
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.VedtakDto
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class NyVedtaksperiodeForkastetMessageTest {

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "fnr"
        private const val SNAPSHOT = "json"
        private val vedtak = VedtakDto(1, 2)
    }

    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, "aktørid")
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
    private val testhendelsefabrikk =
        Hendelsefabrikk(
            vedtakDao = vedtakDao,
            commandContextDao = commandContextDao,
            snapshotDao = snapshotDao,
            risikovurderingDao = risikovurderingDao,
            speilSnapshotRestClient = restClient,
            personDao = mockk(),
            arbeidsgiverDao = mockk(),
            reservasjonsDao = mockk(),
            saksbehandlerDao = mockk(),
            overstyringDao = mockk(),
            oppgaveMediator = mockk(),
            miljøstyrtFeatureToggle = mockk(relaxed = true)
        )
    private val context = CommandContext(CONTEXT)
    private val vedtaksperiodeForkastetMessage = testhendelsefabrikk.nyNyVedtaksperiodeForkastet(
        testmeldingfabrikk.lagVedtaksperiodeForkastet(
            HENDELSE,
            VEDTAKSPERIODE
        )
    )

    @BeforeEach
    fun setup() {
        clearMocks(commandContextDao, vedtakDao, snapshotDao, restClient)
    }

    @Test
    fun `avbryter kommandoer og oppdaterer snapshot`() {
        every { vedtakDao.findVedtak(VEDTAKSPERIODE) } returns vedtak
        every { restClient.hentSpeilSpapshot(FNR) } returns SNAPSHOT
        every { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE, SNAPSHOT) } returns 1
        assertTrue(vedtaksperiodeForkastetMessage.execute(context))
        verify(exactly = 1) { commandContextDao.avbryt(VEDTAKSPERIODE, CONTEXT) }
        verify(exactly = 1) { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE, SNAPSHOT) }
    }

    @Test
    fun `kommando feiler når snapshot feiler`() {
        every { vedtakDao.findVedtak(VEDTAKSPERIODE) } returns vedtak
        every { restClient.hentSpeilSpapshot(FNR) } returns SNAPSHOT
        every { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE, SNAPSHOT) } returns 0
        assertFalse(vedtaksperiodeForkastetMessage.execute(context))
        verify(exactly = 1) { commandContextDao.avbryt(VEDTAKSPERIODE, CONTEXT) }
        verify(exactly = 1) { snapshotDao.oppdaterSnapshotForVedtaksperiode(VEDTAKSPERIODE, SNAPSHOT) }
    }
}
