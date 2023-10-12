package no.nav.helse.spesialist.api.tildeling

import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

interface Oppgavehåndterer {
    fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: SaksbehandlerFraApi)
    fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: SaksbehandlerFraApi)
    fun leggPåVent(oppgaveId: Long, saksbehandler: SaksbehandlerFraApi)
    fun fjernPåVent(oppgaveId: Long, saksbehandler: SaksbehandlerFraApi)
    fun venterPåSaksbehandler(oppgaveId: Long): Boolean
    fun erRiskoppgave(oppgaveId: Long): Boolean
    fun oppgaver(saksbehandlerFraApi: SaksbehandlerFraApi, startIndex: Int, pageSize: Int, sortering: List<Oppgavesortering>): List<OppgaveTilBehandling>
}

class TildelingService(
    private val tildelingDao: TildelingDao,
    private val oppgavehåndterer: Oppgavehåndterer
) {

    internal fun fjernTildeling(oppgaveId: Long): Boolean {
        return tildelingDao.slettTildeling(oppgaveId) > 0
    }

    internal fun leggOppgavePåVent(oppgaveId: Long, saksbehandler: SaksbehandlerFraApi): TildelingApiDto {
        oppgavehåndterer.leggPåVent(oppgaveId, saksbehandler)
        return TildelingApiDto(saksbehandler.navn, saksbehandler.epost, saksbehandler.oid, true)
    }

    internal fun fjernPåVent(oppgaveId: Long, saksbehandler: SaksbehandlerFraApi): TildelingApiDto {
        oppgavehåndterer.fjernPåVent(oppgaveId, saksbehandler)
        return TildelingApiDto(saksbehandler.navn, saksbehandler.epost, saksbehandler.oid, false)
    }

}