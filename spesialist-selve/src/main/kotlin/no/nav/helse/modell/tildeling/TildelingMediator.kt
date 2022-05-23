package no.nav.helse.modell.tildeling

import no.nav.helse.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingDao
import java.util.*

internal class TildelingMediator(
    private val saksbehandlerDao: SaksbehandlerDao,
    private val tildelingDao: TildelingDao,
    private val hendelseMediator: HendelseMediator
) {

    internal fun tildelOppgaveTilSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
        epostadresse: String,
        navn: String,
        ident: String
    ) {
        //TODO: Dette burde gjøres ute i mediatoren
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerreferanse, navn, epostadresse, ident)
        tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandlerreferanse)
    }

    internal fun tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId: Long, saksbehandlerreferanse: UUID) {
        val suksess = hendelseMediator.tildelOppgaveTilSaksbehandler(oppgaveId, saksbehandlerreferanse)
        if (!suksess) {
            val eksisterendeTildeling = tildelingDao.tildelingForOppgave(oppgaveId)
            throw eksisterendeTildeling?.let { OppgaveAlleredeTildelt(eksisterendeTildeling) }
                ?: RuntimeException("Oppgave allerede tildelt, deretter feil ved les av saksbehandlernavn")
        }
    }

    internal fun fjernTildelingOgTildelNySaksbehandlerHvisFinnes(oppgaveId: Long, saksbehandler_oid: UUID?) {
        fjernTildeling(oppgaveId)
        if(saksbehandler_oid != null) {
            tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandler_oid)
        }
    }

    internal fun fjernTildeling(oppgaveId: Long) = tildelingDao.slettTildeling(oppgaveId)
}
