package no.nav.helse.mediator.meldinger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.OppgaveDao
import no.nav.helse.modell.Oppgavestatus
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class OppgaveMakstidPåminnelseTest {
    companion object {
        private const val ID = "006fcaae-4134-11eb-b378-0242ac130002"
        private const val FØDSELSNUMMER = "12020052345"
        private const val OPPGAVE_ID = 1L
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val JSON =
            """{ "@event_name": "påminnelse_oppgave_makstid", "@id": "$ID", "fødselsnummer": "$FØDSELSNUMMER", "oppgaveId": "$OPPGAVE_ID" }"""
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val hendelseDao = mockk<HendelseDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)
    private val hendelsefabrikk = Hendelsefabrikk(
        hendelseDao = hendelseDao,
        reservasjonDao = mockk(),
        saksbehandlerDao = mockk(),
        overstyringDao = mockk(),
        risikovurderingDao = mockk(),
        personDao = mockk(),
        arbeidsgiverDao = mockk(),
        vedtakDao = mockk(),
        warningDao = mockk(),
        commandContextDao = mockk(),
        snapshotDao = mockk(),
        oppgaveDao = oppgaveDao,
        egenAnsattDao = mockk(),
        oppgaveMediator = oppgaveMediator,
        speilSnapshotRestClient = mockk(),
        digitalKontaktinformasjonDao = mockk(),
        åpneGosysOppgaverDao = mockk(),
        miljøstyrtFeatureToggle = mockk(relaxed = true),
        automatisering = mockk(relaxed = true),
        utbetalingDao = mockk(relaxed = true),
        arbeidsforholdDao = mockk(relaxed = true),
        godkjenningMediator = godkjenningMediator,
        opptegnelseDao = mockk(relaxed = true)
    )

    private val oppgaveMakstidPåminnelseMessage = hendelsefabrikk.oppgaveMakstidPåminnelse(
        json = JSON
    )

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `kjører OppgaveMakstidCommand`() {
        every { oppgaveDao.finn(any<Long>()) } returns Oppgave(OPPGAVE_ID, "type", Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
        every { oppgaveDao.venterPåSaksbehandler(any()) } returns true
        every { oppgaveDao.finnMakstid(any()) } returns LocalDateTime.now().minusDays(1)
        assertTrue(oppgaveMakstidPåminnelseMessage.execute(context))

        verify(exactly = 1) { godkjenningMediator.makstidOppnådd(any(), any(), any(), any()) }
        verify(exactly = 1) { oppgaveMediator.makstidOppnådd(any()) }
    }
}
