package no.nav.helse.spesialist.api.tildeling

import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveIkkeTildelt
import no.nav.helse.spesialist.api.oppgave.Oppgavemelder
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import org.slf4j.LoggerFactory

class TildelingService(
    private val tildelingDao: TildelingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    oppgavemelder: () -> Oppgavemelder,
) {
    private val oppgavemelder: Oppgavemelder by lazy { oppgavemelder() }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
        oppgaveId: Long,
        saksbehandlerOid: UUID?,
        saksbehandlerTilganger: SaksbehandlerTilganger
    ) {
        tildelingDao.slettTildeling(oppgaveId)
        if (saksbehandlerOid != null) {
            sikkerlogg.info("Fjerner gammel tildeling og tildeler oppgave $oppgaveId til saksbehandler $saksbehandlerOid")
            tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandlerOid, saksbehandlerTilganger)
        }
    }

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

    internal fun fjernTildeling(oppgaveId: Long): Boolean {
        return tildelingDao.slettTildeling(oppgaveId) > 0
    }

    internal fun leggOppgavePåVent(oppgaveId: Long): TildelingApiDto {
        tildelingDao.tildelingForOppgave(oppgaveId) ?: throw OppgaveIkkeTildelt(oppgaveId)
        val tildeling = tildelingDao.leggOppgavePåVent(oppgaveId)
            ?: throw RuntimeException("Kunne ikke legge på vent")
        oppgavemelder.sendOppgaveOppdatertMelding(oppgaveId)
        return tildeling
    }

    internal fun fjernPåVent(oppgaveId: Long): TildelingApiDto {
        val tildeling = tildelingDao.fjernPåVent(oppgaveId)
            ?: throw RuntimeException("Kunne ikke fjerne fra på vent")
        oppgavemelder.sendOppgaveOppdatertMelding(oppgaveId)
        return tildeling
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
        if ("dev-gcp" == System.getenv("NAIS_CLUSTER_NAME")) return
        totrinnsvurderingApiDao.hentAktiv(oppgaveId)
            ?.takeIf { !it.erRetur && it.saksbehandler != null }
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
