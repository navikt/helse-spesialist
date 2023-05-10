package no.nav.helse.spesialist.api.tildeling

import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.spesialist.api.oppgave.Oppgavemelder
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao

class TildelingService(
    private val tildelingDao: TildelingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val oppgavemelder: Oppgavemelder,
) {
    internal fun tildelOppgaveTilSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
        epostadresse: String,
        navn: String,
        ident: String,
        saksbehandlerTilganger: SaksbehandlerTilganger
    ): TildelingApiDto {
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerreferanse, navn, epostadresse, ident)
        return tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandlerreferanse, saksbehandlerTilganger)
    }

    internal fun fjernTildeling(oppgaveId: Long) {
        tildelingDao.slettTildeling(oppgaveId)
    }

    private fun tildelOppgaveTilEksisterendeSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
        saksbehandlerTilganger: SaksbehandlerTilganger
    ): TildelingApiDto {
        kanTildele(oppgaveId, saksbehandlerreferanse, saksbehandlerTilganger)
        val tildeling = tildelOppgaveTilSaksbehandler(oppgaveId, saksbehandlerreferanse)
        if (tildeling == null) {
            val eksisterendeTildeling = tildelingDao.tildelingForOppgave(oppgaveId)
            throw eksisterendeTildeling?.let { OppgaveAlleredeTildelt(eksisterendeTildeling) }
                ?: RuntimeException("Oppgave allerede tildelt, deretter feil ved les av saksbehandlernavn")
        }
        return tildeling
    }

    private fun kanTildele(oppgaveId: Long, saksbehandlerreferanse: UUID, saksbehandlerTilganger: SaksbehandlerTilganger) {
        totrinnsvurderingApiDao.hentAktiv(oppgaveId)
            ?.takeIf { "dev-gcp" != System.getenv("NAIS_CLUSTER_NAME") && (!it.erRetur && it.saksbehandler != null) }
            ?.let { totrinnsvurdering ->
                check(totrinnsvurdering.saksbehandler != saksbehandlerreferanse) {
                    "Oppgave er beslutteroppgave, og kan ikke attesteres av samme saksbehandler som sendte til godkjenning"
                }
                check(saksbehandlerTilganger.harTilgangTilBeslutterOppgaver()) {
                    "Saksbehandler har ikke beslutter-tilgang"
                }
            }
    }

    private fun tildelOppgaveTilSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
    ): TildelingApiDto? {
        val tildeling = tildelingDao.opprettTildeling(oppgaveId, saksbehandlerreferanse)
        if (tildeling != null) {
            oppgavemelder.sendOppgaveOppdatertMelding(oppgaveId)
        }
        return tildeling
    }

}
