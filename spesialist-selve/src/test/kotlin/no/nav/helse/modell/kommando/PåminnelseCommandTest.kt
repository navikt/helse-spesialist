package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.oppgave.OppgaveMediator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class PåminnelseCommandTest {

    private companion object {
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
    }
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val context = CommandContext(CONTEXT)

    private val command = PåminnelseCommand(VEDTAKSPERIODE, oppgaveMediator)

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveMediator)
    }

    @Test
    fun `avbryter oppgave dersom den venter på saksbehandler og vedtaksperioden ikke er i tilstand AVVENTER_GODKJENNING`() {
        every { oppgaveMediator.venterPåSaksbehandler(VEDTAKSPERIODE) } returns true
        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.avbrytOppgaver(VEDTAKSPERIODE) }
    }

    @Test
    fun `avbryter ikke oppgave dersom vedtaksperioden har oppgave`() {
        every { oppgaveMediator.venterPåSaksbehandler(VEDTAKSPERIODE) } returns false
        assertTrue(command.execute(context))
        verify(exactly = 0) { oppgaveMediator.avbrytOppgaver(VEDTAKSPERIODE) }
    }
}
