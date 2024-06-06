package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.CommandContextDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AvbrytCommandTest {
    private companion object {
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
    }

    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val context = CommandContext(CONTEXT)

    private val command = AvbrytCommand(VEDTAKSPERIODE, commandContextDao, oppgaveService)

    @BeforeEach
    fun setup() {
        clearMocks(commandContextDao)
    }

    @Test
    fun `avbryter command context`() {
        assertTrue(command.execute(context))
        verify(exactly = 1) { commandContextDao.avbryt(VEDTAKSPERIODE, CONTEXT) }
    }
}
