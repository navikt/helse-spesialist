package no.nav.helse.spesialist.api.oppgave

import no.nav.helse.spesialist.api.graphql.schema.BehandletOppgave
import no.nav.helse.spesialist.api.graphql.schema.Fane
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi

interface Oppgavehåndterer {
    fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: SaksbehandlerFraApi)
    fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: SaksbehandlerFraApi)
    fun venterPåSaksbehandler(oppgaveId: Long): Boolean
    fun erRiskoppgave(oppgaveId: Long): Boolean
    fun oppgaver(saksbehandlerFraApi: SaksbehandlerFraApi, startIndex: Int, pageSize: Int, sortering: List<Oppgavesortering>, egenskaper: List<Oppgaveegenskap>, fane: Fane): List<OppgaveTilBehandling>
    fun behandledeOppgaver(saksbehandlerFraApi: SaksbehandlerFraApi): List<BehandletOppgave>
}