package no.nav.helse.spesialist.api.tildeling

import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao

interface IOppgavemelder {
    fun sendOppgaveOppdatertMelding(oppgaveId: Long)
}

interface Oppgavehåndterer {
    fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: SaksbehandlerFraApi)
    fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: SaksbehandlerFraApi)
    fun leggPåVent(oppgaveId: Long): TildelingApiDto
    fun fjernPåVent(oppgaveId: Long)
    fun venterPåSaksbehandler(oppgaveId: Long): Boolean
    fun erRiskoppgave(oppgaveId: Long): Boolean
    fun oppgaver(saksbehandlerFraApi: SaksbehandlerFraApi, startIndex: Int, pageSize: Int, sortering: List<Oppgavesortering>): List<OppgaveTilBehandling>
}

class TildelingService(
    private val tildelingDao: TildelingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val oppgavehåndterer: Oppgavehåndterer,
    oppgavemelder: () -> IOppgavemelder
) {
    private val oppgavemelder: IOppgavemelder by lazy { oppgavemelder() }

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
        return oppgavehåndterer.leggPåVent(oppgaveId)
    }

    internal fun fjernPåVent(oppgaveId: Long, saksbehandler: SaksbehandlerFraApi): TildelingApiDto {
        oppgavehåndterer.fjernPåVent(oppgaveId)
        return TildelingApiDto(saksbehandler.navn, saksbehandler.epost, saksbehandler.oid, false)
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