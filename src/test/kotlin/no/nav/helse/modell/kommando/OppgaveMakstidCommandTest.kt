package no.nav.helse.modell.kommando

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.*
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

internal class OppgaveMakstidCommandTest {
    private companion object {
        private const val OPPGAVE_ID = 1L
        private const val FNR = "12345678901"
        private val FORTID = LocalDateTime.now().minusDays(1)
        private val FREMTID = LocalDateTime.now().plusDays(1)
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val oppgave = Oppgave(OPPGAVE_ID, "", Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
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
        every { oppgaveDao.venterPåSaksbehandler(OPPGAVE_ID) } returns true
        every { oppgaveDao.finnMakstid(OPPGAVE_ID) } returns FORTID
        every { oppgaveDao.finn(OPPGAVE_ID) } returns oppgave
        every { oppgaveDao.finnHendelseId(OPPGAVE_ID) } returns godkjenningsbehovhendelseId
        every { hendelseDao.finnUtbetalingsgodkjenningbehov(godkjenningsbehovhendelseId) } returns behov

        assertTrue(command.execute(commandContext))
        verify(exactly = 1) { oppgaveDao.venterPåSaksbehandler(OPPGAVE_ID) }
        verify(exactly = 1) { oppgaveDao.finnMakstid(OPPGAVE_ID) }
        verify(exactly = 1) { oppgaveDao.finn(OPPGAVE_ID) }
        verify(exactly = 1) { oppgaveDao.finnHendelseId(OPPGAVE_ID) }
        verify(exactly = 1) { hendelseDao.finnUtbetalingsgodkjenningbehov(godkjenningsbehovhendelseId) }
        verify(exactly = 1) { godkjenningMediator.makstidOppnådd(commandContext, behov, VEDTAKSPERIODE_ID, FNR) }
        verify(exactly = 1) { oppgaveMediator.makstidOppnådd(oppgave) }
    }

    @Test
    fun `ingenting skjer dersom makstid ikke er oppnådd`() {
        every { oppgaveDao.venterPåSaksbehandler(OPPGAVE_ID) } returns true
        every { oppgaveDao.finnMakstid(OPPGAVE_ID) } returns FREMTID
        assertTrue(command.execute(commandContext))
        verify(exactly = 0) { godkjenningMediator.makstidOppnådd(commandContext, behov, VEDTAKSPERIODE_ID, FNR) }
        verify(exactly = 0) { oppgaveMediator.makstidOppnådd(oppgave) }
    }

    @Test
    fun `kafka-melding 'oppgave_oppdatert' sendes dersom oppgaven er inaktiv`() {
        every { oppgaveDao.finn(any<Long>()) } returns Oppgave(OPPGAVE_ID, "type", Oppgavestatus.Invalidert, VEDTAKSPERIODE_ID)
        every { oppgaveDao.finnFødselsnummer(any()) } returns FNR
        every { oppgaveDao.finnMakstid(any()) } returns FREMTID
        assertTrue(command.execute(commandContext))
        val melding = objectMapper.readTree(commandContext.meldinger()[0])
        assertEquals("oppgave_oppdatert", melding["@event_name"].asText())
        assertEquals(OPPGAVE_ID, melding["oppgaveId"].asLong())
        assertEquals("Invalidert", melding["status"].asText())
        assertEquals(FNR, melding["fødselsnummer"].asText())
        assertEquals(FREMTID, melding["makstid"].asLocalDateTime())
    }
}
