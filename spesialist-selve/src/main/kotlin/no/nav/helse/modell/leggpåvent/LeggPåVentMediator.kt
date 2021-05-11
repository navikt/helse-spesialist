package no.nav.helse.modell.leggpåvent

import no.nav.helse.feilhåndtering.Modellfeil
import no.nav.helse.feilhåndtering.OppgaveErIkkeTildelt
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.tildeling.TildelingDao

internal class LeggPåVentMediator(
    private val tildelingDao: TildelingDao,
    private val hendelseMediator: HendelseMediator
) {

    internal fun leggOppgavePåVent(
        oppgaveId: Long
    ) {
        tildelingDao.tildelingForOppgave(oppgaveId) ?: throw Modellfeil(OppgaveErIkkeTildelt(oppgaveId))
        tildelingDao.leggOppgavePåVent(oppgaveId)
        hendelseMediator.leggOppgavePåVent(oppgaveId)
    }

    internal fun fjernPåVent(oppgaveId: Long) = tildelingDao.fjernPåVent(oppgaveId)
}
