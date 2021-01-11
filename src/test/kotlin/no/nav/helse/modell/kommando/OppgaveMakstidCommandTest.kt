package no.nav.helse.modell.kommando

import io.mockk.*
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class OppgaveMakstidCommandTest {
    private companion object {
        private const val OPPGAVE_ID = 1L
        private const val FNR = "12345678901"
        private val FORTID = LocalDateTime.now().minusDays(1)
        private val FREMTID = LocalDateTime.now().plusDays(1)
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val oppgave = Oppgave(OPPGAVE_ID, "Et navn", Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
        private val godkjenningsbehovhendelseId = UUID.randomUUID()
    }

    private val behov = UtbetalingsgodkjenningMessage("""{ "@event_name": "behov" }""")
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val hendelseDao = mockk<HendelseDao>(relaxed = true)

    private lateinit var command: OppgaveMakstidCommand
    private lateinit var commandContext: CommandContext

    @BeforeEach
    fun setup() {
        clearAllMocks()
        command = OppgaveMakstidCommand(OPPGAVE_ID, FNR, VEDTAKSPERIODE_ID, oppgaveDao, hendelseDao, godkjenningMediator, oppgaveMediator)
        commandContext = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `ved makstid oppnådd sendes løsning på Godkjenningsbehov med godkjent=false og oppdaterer oppgavestatus til MakstidOppnådd`() {
        every { oppgaveDao.erAktivOppgave(OPPGAVE_ID) } returns true
        every { oppgaveDao.finnMakstid(OPPGAVE_ID) } returns FORTID
        every { oppgaveDao.finn(OPPGAVE_ID) } returns oppgave
        every { oppgaveDao.finnHendelseId(OPPGAVE_ID) } returns godkjenningsbehovhendelseId
        every { hendelseDao.finnUtbetalingsgodkjenningbehov(godkjenningsbehovhendelseId) } returns behov

        assertTrue(command.execute(commandContext))
        verify(exactly = 1) { oppgaveDao.erAktivOppgave(OPPGAVE_ID) }
        verify(exactly = 1) { oppgaveDao.finnMakstid(OPPGAVE_ID) }
        verify(exactly = 1) { oppgaveDao.finn(OPPGAVE_ID) }
        verify(exactly = 1) { oppgaveDao.finnHendelseId(OPPGAVE_ID) }
        verify(exactly = 1) { hendelseDao.finnUtbetalingsgodkjenningbehov(godkjenningsbehovhendelseId) }
        verify(exactly = 1) { godkjenningMediator.makstidOppnådd(commandContext, behov, VEDTAKSPERIODE_ID, FNR) }
        verify(exactly = 1) { oppgaveMediator.makstidOppnådd(oppgave) }
    }

    @Test
    fun `ingenting skjer dersom makstid ikke er oppnådd`() {
        every { oppgaveDao.erAktivOppgave(OPPGAVE_ID) } returns true
        every { oppgaveDao.finnMakstid(OPPGAVE_ID) } returns FREMTID
        assertTrue(command.execute(commandContext))
        verify(exactly = 0) { godkjenningMediator.makstidOppnådd(commandContext, behov, VEDTAKSPERIODE_ID, FNR) }
        verify(exactly = 0) { oppgaveMediator.makstidOppnådd(oppgave) }
    }

    @Test
    fun `ingenting skjer dersom oppgavestatus ikke er AvventerSaksbehandler`() {
        every { oppgaveDao.erAktivOppgave(OPPGAVE_ID) } returns false
        every { oppgaveDao.finnMakstid(OPPGAVE_ID) } returns FORTID
        assertTrue(command.execute(commandContext))
        verify(exactly = 0) { godkjenningMediator.makstidOppnådd(commandContext, behov, VEDTAKSPERIODE_ID, FNR) }
        verify(exactly = 0) { oppgaveMediator.makstidOppnådd(oppgave) }
    }
}
