package no.nav.helse.modell.oppgave

import no.nav.helse.mediator.oppgave.Oppgavefinner
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_GODKJENT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT
import java.util.UUID

internal class OppdaterOppgavestatusCommand(
    private val utbetalingId: UUID,
    private val status: Utbetalingsstatus,
    private val oppgavefinner: Oppgavefinner,
) : Command() {
    override fun execute(context: CommandContext): Boolean {
        oppgavefinner.oppgave(utbetalingId) {
            when (status) {
                GODKJENT_UTEN_UTBETALING,
                UTBETALT,
                IKKE_GODKJENT,
                -> ferdigstill()
                FORKASTET -> avbryt()
                else -> Unit
            }
        }
        return true
    }
}
