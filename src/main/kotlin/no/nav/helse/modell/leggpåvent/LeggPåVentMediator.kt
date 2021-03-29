package no.nav.helse.modell.leggpåvent

import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.OppgaveDao
import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErIkkeTildelt
import no.nav.helse.modell.tildeling.TildelingDao

internal class LeggPåVentMediator(
    private val tildelingDao: TildelingDao,
    private val oppgaveDao: OppgaveDao,
    private val hendelseMediator: HendelseMediator
) {

    internal fun leggOppgavePåVent(
        oppgaveId: Long
    ) {
        val saksbehandlerFor = tildelingDao.finnSaksbehandlerNavn(oppgaveId)
        if (saksbehandlerFor == null) {
            throw ModellFeil(OppgaveErIkkeTildelt(oppgaveId))
        }
        tildelingDao.leggOppgavePåVent(oppgaveId)
        hendelseMediator.leggOppgavePåVent(oppgaveId)
    }

    internal fun fjernPåVent(oppgaveId: Long) = tildelingDao.fjernPåVent(oppgaveId)
}
