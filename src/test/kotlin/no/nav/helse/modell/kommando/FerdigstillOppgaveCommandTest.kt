package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.OppgaveDao
import no.nav.helse.modell.Oppgavestatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class FerdigstillOppgaveCommandTest {
    private companion object {
        private const val IDENT = "Z999999"
        private val OID = UUID.randomUUID()
        private const val OPPGAVE_ID = 1L
        private val oppgave = Oppgave(OPPGAVE_ID, "Et navn", Oppgavestatus.AvventerSaksbehandler, UUID.randomUUID())
    }
    private val mediator = mockk<OppgaveMediator>(relaxed = true)
    private val dao = mockk<OppgaveDao>(relaxed = true)

    private lateinit var command: FerdigstillOppgaveCommand
    private lateinit var commandContext: CommandContext

    @BeforeEach
    fun setup() {
        clearMocks(mediator)
        command = FerdigstillOppgaveCommand(mediator, IDENT, OID, OPPGAVE_ID, dao)
        commandContext = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `ferdigstiller oppgave`() {
        every { dao.finn(OPPGAVE_ID) } returns oppgave
        assertTrue(command.execute(commandContext))
        verify(exactly = 1) { dao.finn(OPPGAVE_ID) }
        verify(exactly = 1) { mediator.ferdigstill(oppgave, IDENT, OID) }
    }

    @Test
    fun `oppgaver finnes ikke`() {
        every { dao.finn(OPPGAVE_ID) } returns null
        assertThrows<IllegalArgumentException> { (command.execute(commandContext)) }
    }
}
