package no.nav.helse.modell.tildeling

import java.util.UUID
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

internal class TildelingService(
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

    private fun tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId: Long, saksbehandlerreferanse: UUID) {
        check(kanTildele(oppgaveId, saksbehandlerreferanse)) {
            "Oppgave er beslutteroppgave, og kan ikke attesteres av samme saksbehandler som sendte til godkjenning"
        }
        val suksess = hendelseMediator.tildelOppgaveTilSaksbehandler(oppgaveId, saksbehandlerreferanse)
        if (!suksess) {
            val eksisterendeTildeling = tildelingDao.tildelingForOppgave(oppgaveId)
            throw eksisterendeTildeling?.let { OppgaveAlleredeTildelt(eksisterendeTildeling) }
                ?: RuntimeException("Oppgave allerede tildelt, deretter feil ved les av saksbehandlernavn")
        }
    }

    private fun kanTildele(oppgaveId: Long, saksbehandlerreferanse: UUID) =
        "dev-gcp" == System.getenv("NAIS_CLUSTER_NAME")
                || !hendelseMediator.erBeslutteroppgaveOgErTidligereSaksbehandler(oppgaveId, saksbehandlerreferanse)

    internal fun fjernTildelingOgTildelNySaksbehandlerHvisFinnes(oppgaveId: Long, saksbehandler_oid: UUID?) {
        fjernTildeling(oppgaveId)
        if (saksbehandler_oid != null) {
            sikkerLog.info("Fjerner gammel tildeling og tildeler oppgave $oppgaveId til saksbehandler $saksbehandler_oid")
            tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandler_oid)
        }
    }

    internal fun fjernTildeling(oppgaveId: Long) = tildelingDao.slettTildeling(oppgaveId)
}
