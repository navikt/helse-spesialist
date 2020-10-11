package no.nav.helse.mediator.meldinger

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.modell.*
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.VedtakDto
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.snapshot
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class VedtaksperiodeForkastetTest {

    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "fnr"
        private val SNAPSHOT = snapshot(VEDTAKSPERIODE)
        private val vedtak = VedtakDto(1, 2)
    }

    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, "aktørid")
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
    private val testhendelsefabrikk =
        Hendelsefabrikk(
            hendelseDao = mockk(),
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            oppgaveDao = oppgaveDao,
            commandContextDao = commandContextDao,
            snapshotDao = snapshotDao,
            risikovurderingDao = risikovurderingDao,
            personDao = mockk(),
            arbeidsgiverDao = mockk(),
            reservasjonDao = mockk(),
            saksbehandlerDao = mockk(),
            overstyringDao = mockk(),
            digitalKontaktinformasjonDao = mockk(),
            åpneGosysOppgaverDao = mockk(),
            oppgaveMediator = mockk(),
            speilSnapshotRestClient = restClient,
            miljøstyrtFeatureToggle = mockk(relaxed = true),
            automatisering = mockk(relaxed = true)
        )
    private val context = CommandContext(CONTEXT)
    private val vedtaksperiodeForkastetMessage = testhendelsefabrikk.vedtaksperiodeForkastet(
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
