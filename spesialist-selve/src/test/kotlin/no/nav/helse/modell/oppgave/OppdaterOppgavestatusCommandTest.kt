package no.nav.helse.modell.oppgave

import java.util.UUID
import no.nav.helse.mediator.oppgave.Oppgavefinner
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.oppgave.Egenskap.SØKNAD
import no.nav.helse.modell.oppgave.OppgaveInspector.Companion.oppgaveinspektør
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_GODKJENT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OppdaterOppgavestatusCommandTest {

    private val UTBETALING_ID = UUID.randomUUID()
    private val context = CommandContext(UUID.randomUUID())
    private lateinit var oppgave: Oppgave

    @BeforeEach
    fun beforeEach() {
        oppgave = Oppgave.nyOppgave(
            id = 1L,
            vedtaksperiodeId = UUID.randomUUID(),
            utbetalingId = UTBETALING_ID,
            hendelseId = UUID.randomUUID(),
            egenskaper = listOf(SØKNAD)
        )
    }

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt utbetalt`() {
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val status = UTBETALT
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

        assertTrue(command.execute(context))
        oppgaveinspektør(oppgave) {
            assertEquals(Oppgave.Ferdigstilt, this.tilstand)
        }
    }

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt godkjent uten utbetaling`() {
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val status = GODKJENT_UTEN_UTBETALING
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

        assertTrue(command.execute(context))
        oppgaveinspektør(oppgave) {
            assertEquals(Oppgave.Ferdigstilt, this.tilstand)
        }

    }

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt avslått`() {
        val status = IKKE_GODKJENT
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

        assertTrue(command.execute(context))
        oppgaveinspektør(oppgave) {
            assertEquals(Oppgave.Ferdigstilt, this.tilstand)
        }
    }

    @Test
    fun `invaliderer oppgave basert på utbetalingens status`() {
        val status = FORKASTET
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

        assertTrue(command.execute(context))
        oppgaveinspektør(oppgave) {
            assertEquals(Oppgave.Invalidert, this.tilstand)
        }
    }

    @Test
    fun `gjør ingenting om vi ikke forholder oss til utbetalingsstatusen`() {
        oppgave.avventerSystem("IDENT", UUID.randomUUID())
        val status = ANNULLERT
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

        assertTrue(command.execute(context))
        oppgaveinspektør(oppgave) {
            assertEquals(Oppgave.AvventerSystem, this.tilstand)
        }
    }

    private val oppgavehåndterer get() = object : Oppgavefinner {
        override fun oppgave(utbetalingId: UUID, oppgaveBlock: Oppgave?.() -> Unit) {
            oppgaveBlock(oppgave)
        }
    }
}
