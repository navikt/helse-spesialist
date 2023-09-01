package no.nav.helse.modell.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.ANNULLERT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_GODKJENT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OppdaterOppgavestatusCommandTest {

    private val UTBETALING_ID = UUID.randomUUID()
    private val context = CommandContext(UUID.randomUUID())
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt utbetalt`() {
        val oppgave = Oppgave(
            1L,
            Oppgavetype.SØKNAD,
            Oppgavestatus.AvventerSystem,
            UUID.randomUUID(),
            UTBETALING_ID,
            "IDENT",
            UUID.randomUUID()
        )
        every { oppgaveDao.finn(any<UUID>()) } returns oppgave
        val status = UTBETALT
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgaveDao, oppgaveMediator)

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.ferdigstill(oppgave) }
    }

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt godkjent uten utbetaling`() {
        val oppgave = Oppgave(
            1L,
            Oppgavetype.SØKNAD,
            Oppgavestatus.AvventerSystem,
            UUID.randomUUID(),
            UTBETALING_ID,
            "IDENT",
            UUID.randomUUID()
        )
        every { oppgaveDao.finn(any<UUID>()) } returns oppgave
        val status = GODKJENT_UTEN_UTBETALING
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgaveDao, oppgaveMediator)

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.ferdigstill(oppgave) }
    }

    @Test
    fun `ferdigstiller oppgave når utbetalingen har blitt avslått`() {
        val oppgave = Oppgave(
            1L,
            Oppgavetype.SØKNAD,
            Oppgavestatus.AvventerSystem,
            UUID.randomUUID(),
            UTBETALING_ID,
            "IDENT",
            UUID.randomUUID()
        )
        every { oppgaveDao.finn(any<UUID>()) } returns oppgave
        val status = IKKE_GODKJENT
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgaveDao, oppgaveMediator)

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.ferdigstill(oppgave) }
        verify(exactly = 0) { oppgaveMediator.invalider(oppgave) }
    }

    @Test
    fun `invaliderer oppgave basert på utbetalingens status`() {
        val oppgave = Oppgave(
            1L,
            Oppgavetype.SØKNAD,
            Oppgavestatus.AvventerSystem,
            UUID.randomUUID(),
            UTBETALING_ID,
            "IDENT",
            UUID.randomUUID()
        )
        every { oppgaveDao.finn(any<UUID>()) } returns oppgave
        val status = FORKASTET
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgaveDao, oppgaveMediator)

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.invalider(oppgave) }
        verify(exactly = 0) { oppgaveMediator.ferdigstill(oppgave) }
    }

    @Test
    fun `gjør ingenting om vi ikke forholder oss til utbetalingsstatusen`() {
        val oppgave = Oppgave(
            1L,
            Oppgavetype.SØKNAD,
            Oppgavestatus.AvventerSystem,
            UUID.randomUUID(),
            UTBETALING_ID,
            "IDENT",
            UUID.randomUUID()
        )
        every { oppgaveDao.finn(any<UUID>()) } returns oppgave
        val status = ANNULLERT
        val command = OppdaterOppgavestatusCommand(UTBETALING_ID, status, oppgaveDao, oppgaveMediator)

        assertTrue(command.execute(context))
        verify(exactly = 0) { oppgaveMediator.invalider(oppgave) }
        verify(exactly = 0) { oppgaveMediator.ferdigstill(oppgave) }
    }
}
