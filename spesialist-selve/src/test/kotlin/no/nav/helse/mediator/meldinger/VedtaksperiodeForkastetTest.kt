package no.nav.helse.mediator.meldinger

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.snapshotUtenWarnings
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
        private val SNAPSHOT = snapshotUtenWarnings(vedtaksperiodeId = VEDTAKSPERIODE, orgnr = "heisann",fnr = FNR, aktørId = "asdf")
    }

    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, "aktørid")
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val snapshotDao = mockk<SnapshotDao>(relaxed = true)
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)
    private val risikovurderingDao = mockk<RisikovurderingDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val testhendelsefabrikk =
        Hendelsefabrikk(
            hendelseDao = mockk(),
            personDao = mockk(),
            arbeidsgiverDao = mockk(),
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            oppgaveDao = oppgaveDao,
            commandContextDao = commandContextDao,
            snapshotDao = snapshotDao,
            reservasjonDao = mockk(),
            tildelingDao = mockk(),
            saksbehandlerDao = mockk(),
            overstyringDao = mockk(),
            risikovurderingDao = risikovurderingDao,
            digitalKontaktinformasjonDao = mockk(),
            åpneGosysOppgaverDao = mockk(),
            egenAnsattDao = mockk(),
            speilSnapshotRestClient = restClient,
            oppgaveMediator = oppgaveMediator,
            godkjenningMediator = mockk(relaxed = true),
            automatisering = mockk(relaxed = true),
            arbeidsforholdDao = mockk(relaxed = true),
            utbetalingDao = mockk(relaxed = true),
            opptegnelseDao = mockk(relaxed = true)
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
        every { restClient.hentSpeilSpapshot(FNR) } returns SNAPSHOT
        every { snapshotDao.lagre(FNR, SNAPSHOT) } returns 1
        assertTrue(vedtaksperiodeForkastetMessage.execute(context))
        verify(exactly = 1) { commandContextDao.avbryt(VEDTAKSPERIODE, CONTEXT) }
        verify(exactly = 1) { snapshotDao.lagre(FNR, SNAPSHOT) }
    }
}
