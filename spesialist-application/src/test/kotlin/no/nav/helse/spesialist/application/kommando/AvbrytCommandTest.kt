package no.nav.helse.spesialist.application.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.CommandContextDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AvbrytCommandTest : ApplicationTest() {
    private companion object {
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()

        private fun lagAvbrytCommand(commandContextDao: CommandContextDao) =
            AvbrytCommand(
                fødselsnummer = "fnr",
                vedtaksperiodeId = VEDTAKSPERIODE,
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                commandContextDao = commandContextDao,
                oppgaveService = mockk<OppgaveService>(relaxed = true),
                reservasjonDao = mockk(relaxed = true),
                tildelingDao = mockk(relaxed = true),
                totrinnsvurderingRepository = mockk(relaxed = true),
            )
    }

    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val context = CommandContext(CONTEXT)

    private val command = lagAvbrytCommand(commandContextDao)

    @Test
    fun `avbryter command context`() {
        assertTrue(command.execute(context, sessionContext, outbox))
        verify(exactly = 1) { commandContextDao.avbryt(VEDTAKSPERIODE, CONTEXT) }
    }
}
