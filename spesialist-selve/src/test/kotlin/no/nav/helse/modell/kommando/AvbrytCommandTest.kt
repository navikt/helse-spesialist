package no.nav.helse.modell.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.CommandContextDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AvbrytCommandTest {
    private companion object {
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private fun lagAvbrytCommand(fødselsnummer: String, vedtaksperiodeId: UUID, commandContextDao: CommandContextDao) = AvbrytCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            commandContextDao = commandContextDao,
            oppgaveService = mockk<OppgaveService>(relaxed = true),
            reservasjonDao = mockk(relaxed = true),
            tildelingDao = mockk(relaxed = true),
            oppgaveDao = mockk(relaxed = true),
            totrinnsvurderingMediator = mockk(relaxed = true)
        )
    }
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val context = CommandContext(CONTEXT)

    private val command = lagAvbrytCommand("fnr", VEDTAKSPERIODE, commandContextDao)

    @Test
    fun `avbryter command context`() {
        assertTrue(command.execute(context))
        verify(exactly = 1) { commandContextDao.avbryt(VEDTAKSPERIODE, CONTEXT) }
    }
}
