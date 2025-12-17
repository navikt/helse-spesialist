package no.nav.helse.spesialist.application.kommando

import no.nav.helse.mediator.oppgave.Oppgavefinner
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.OppdaterOppgavestatusCommand
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.spesialist.application.modell.OppgaveInspector
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OppdaterOppgavestatusCommandTest {
    private val UTBETALING_ID = UUID.randomUUID()
    private val context = CommandContext(UUID.randomUUID())
    private lateinit var oppgave: Oppgave

    @BeforeEach
    fun beforeEach() {
        oppgave =
            Oppgave.ny(
                id = 1L,
                førsteOpprettet = null,
                vedtaksperiodeId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                utbetalingId = UTBETALING_ID,
                hendelseId = UUID.randomUUID(),
                kanAvvises = true,
                egenskaper = setOf(Egenskap.`SØKNAD`),
            )
    }

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt utbetalt`() =
        testMedSessionContext {
            oppgave.avventerSystem(lagSaksbehandler().ident, UUID.randomUUID())
            val status = Utbetalingsstatus.UTBETALT
            val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

            Assertions.assertTrue(command.execute(context, it))
            OppgaveInspector.`oppgaveinspektør`(oppgave) {
                Assertions.assertEquals(Oppgave.Ferdigstilt, this.tilstand)
            }
        }

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt godkjent uten utbetaling`() =
        testMedSessionContext {
            oppgave.avventerSystem(lagSaksbehandler().ident, UUID.randomUUID())
            val status = Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
            val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

            Assertions.assertTrue(command.execute(context, it))
            OppgaveInspector.`oppgaveinspektør`(oppgave) {
                Assertions.assertEquals(Oppgave.Ferdigstilt, this.tilstand)
            }
        }

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt avslått`() =
        testMedSessionContext {
            val status = Utbetalingsstatus.IKKE_GODKJENT
            oppgave.avventerSystem(lagSaksbehandler().ident, UUID.randomUUID())
            val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

            Assertions.assertTrue(command.execute(context, it))
            OppgaveInspector.`oppgaveinspektør`(oppgave) {
                Assertions.assertEquals(Oppgave.Ferdigstilt, this.tilstand)
            }
        }

    @Test
    fun `invaliderer oppgave basert på utbetalingens status`() =
        testMedSessionContext {
            val status = Utbetalingsstatus.FORKASTET
            val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

            Assertions.assertTrue(command.execute(context, it))
            OppgaveInspector.`oppgaveinspektør`(oppgave) {
                Assertions.assertEquals(Oppgave.Invalidert, this.tilstand)
            }
        }

    @Test
    fun `gjør ingenting om vi ikke forholder oss til utbetalingsstatusen`() =
        testMedSessionContext {
            oppgave.avventerSystem(lagSaksbehandler().ident, UUID.randomUUID())
            val status = Utbetalingsstatus.ANNULLERT
            val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgavehåndterer)

            Assertions.assertTrue(command.execute(context, it))
            OppgaveInspector.`oppgaveinspektør`(oppgave) {
                Assertions.assertEquals(Oppgave.AvventerSystem, this.tilstand)
            }
        }

    private val oppgavehåndterer get() =
        object : Oppgavefinner {
            override fun oppgave(
                utbetalingId: UUID,
                oppgaveBlock: Oppgave.() -> Unit,
            ) {
                oppgaveBlock(oppgave)
            }
        }
}
