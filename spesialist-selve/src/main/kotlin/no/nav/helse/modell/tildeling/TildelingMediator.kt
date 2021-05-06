package no.nav.helse.modell.tildeling

import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
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
        navn: String
    ) {

        val eksisterendeTildeling = tildelingDao.tildelingForOppgave(oppgaveId)
        if (eksisterendeTildeling != null) {
            throw ModellFeil(OppgaveErAlleredeTildelt(eksisterendeTildeling))
        }
        //TODO: Dette burde gjøres ute i mediatoren
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerreferanse, navn, epostadresse)

        //TODO: Her burde egentlig tildelOppgave kaste en feil om den allerede er tildelt som vi kan fange og håndtere på en fin måte.
        // Da slipper vi den raceconditionen vi har her om to tråder begge sjekker for tildeling samtidig og så tildeler samtidig
        hendelseMediator.tildelOppgaveTilSaksbehandler(oppgaveId, saksbehandlerreferanse)
    }

    internal fun fjernTildeling(oppgaveId: Long) = tildelingDao.slettTildeling(oppgaveId)
}
