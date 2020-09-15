package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.command.OppgaveDao
import java.util.*

internal class AvbrytOppgaveCommand(
    private val vedtaksperiodeId: UUID,
    private val oppgaveDao: OppgaveDao
) : Command {

    override fun execute(context: CommandContext): Boolean {
        oppgaveDao.invaliderOppgaver(vedtaksperiodeId)
        return true
    }

}
