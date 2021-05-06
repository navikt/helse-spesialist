package no.nav.helse.modell.oppgave

import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.*
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import java.util.*

internal class OppdaterOppgavestatusCommand(
    private val utbetalingId: UUID,
    private val status: Utbetalingsstatus,
    private val oppgaveDao: OppgaveDao,
    private val oppgaveMediator: OppgaveMediator
) : Command {
    override fun execute(context: CommandContext): Boolean {
        oppgaveDao.finn(utbetalingId)?.also {
            when (status) {
                UTBETALT,
                IKKE_GODKJENT -> oppgaveMediator.ferdigstill(it)
                FORKASTET -> oppgaveMediator.invalider(it)
                else -> Unit
            }
        }
        return true
    }
}
