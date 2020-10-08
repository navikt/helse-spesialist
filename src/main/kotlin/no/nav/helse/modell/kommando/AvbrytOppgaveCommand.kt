package no.nav.helse.modell.kommando

import no.nav.helse.modell.OppgaveDao
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
