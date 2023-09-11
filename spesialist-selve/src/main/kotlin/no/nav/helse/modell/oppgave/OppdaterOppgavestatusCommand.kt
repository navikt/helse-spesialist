package no.nav.helse.modell.oppgave

import java.util.UUID
import no.nav.helse.mediator.oppgave.Oppgavefinner
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.FORKASTET
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_GODKJENT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.UTBETALT

internal class OppdaterOppgavestatusCommand(
    private val utbetalingId: UUID,
    private val status: Utbetalingsstatus,
    private val oppgaveMediator: Oppgavefinner
) : Command {
    override fun execute(context: CommandContext): Boolean {
        oppgaveMediator.oppgave(utbetalingId) {
            when (status) {
                GODKJENT_UTEN_UTBETALING,
                UTBETALT,
                IKKE_GODKJENT -> this?.ferdigstill()
                FORKASTET -> this?.avbryt()
                else -> {} // NOP
            }
        }
        return true
    }
}
