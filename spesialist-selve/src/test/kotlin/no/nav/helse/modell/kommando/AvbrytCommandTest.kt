package no.nav.helse.modell.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.mediator.oppgave.OppgaveService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AvbrytCommandTest {
    private companion object {
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private fun lagAvbrytCommand(commandContextRepository: CommandContextRepository) = AvbrytCommand(
            fødselsnummer = "fnr",
            vedtaksperiodeId = VEDTAKSPERIODE,
            commandContextRepository = commandContextRepository,
            oppgaveService = mockk<OppgaveService>(relaxed = true),
            reservasjonRepository = mockk(relaxed = true),
            tildelingRepository = mockk(relaxed = true),
            oppgaveRepository = mockk(relaxed = true),
            totrinnsvurderingMediator = mockk(relaxed = true)
        )
    }
    private val commandContextRepository = mockk<CommandContextRepository>(relaxed = true)
    private val context = CommandContext(CONTEXT)

    private val command = lagAvbrytCommand(commandContextRepository)

    @Test
    fun `avbryter command context`() {
        assertTrue(command.execute(context))
        verify(exactly = 1) { commandContextRepository.avbryt(VEDTAKSPERIODE, CONTEXT) }
    }
}
