package no.nav.helse.modell.command.nyny

import io.mockk.Ordering
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextLong

internal class SaksbehandlerGodkjenningCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val JSON = "{}"
        private const val SAKSBEHANDLER = "Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private const val EPOST = "saksbehandler@nav.no"
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private val OPPGAVE_ID = nextLong()
    }

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private lateinit var context: CommandContext
    private val command = SaksbehandlerGodkjenningCommand(VEDTAKSPERIODE_ID, JSON, oppgaveMediator)
    private lateinit var forventetOppgave: Oppgave

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        forventetOppgave = Oppgave.avventerSaksbehandler(SaksbehandlerGodkjenningCommand::class.java.simpleName, VEDTAKSPERIODE_ID)
        clearMocks(oppgaveMediator)
    }

    @Test
    fun `oppretter oppgave`() {
        assertFalse(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.oppgave(forventetOppgave) }
    }

    @Test
    fun `ferdigstiller oppgave`() {
        context.add(SaksbehandlerLøsning(true, SAKSBEHANDLER, SAKSBEHANDLER_OID, EPOST, GODKJENTTIDSPUNKT, null, null, null, OPPGAVE_ID))
        assertTrue(command.execute(context))
        assertEquals(1, context.meldinger().size)

        verify(ordering = Ordering.SEQUENCE) {
            oppgaveMediator.oppgave(eq(forventetOppgave))
            forventetOppgave.ferdigstill(OPPGAVE_ID, SAKSBEHANDLER, SAKSBEHANDLER_OID)
            oppgaveMediator.oppgave(eq(forventetOppgave))
        }
    }

    @Test
    fun resume() {
        context.add(SaksbehandlerLøsning(true, SAKSBEHANDLER, SAKSBEHANDLER_OID, EPOST, GODKJENTTIDSPUNKT, null, null, null, OPPGAVE_ID))
        assertTrue(command.resume(context))
        assertEquals(1, context.meldinger().size)

        forventetOppgave.ferdigstill(OPPGAVE_ID, SAKSBEHANDLER, SAKSBEHANDLER_OID)
        verify(exactly = 1) { oppgaveMediator.oppgave(eq(forventetOppgave)) }
    }
}
