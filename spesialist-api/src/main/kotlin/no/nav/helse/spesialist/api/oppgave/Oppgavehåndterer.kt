package no.nav.helse.spesialist.api.oppgave

import no.nav.helse.spesialist.api.graphql.schema.AntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.BehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.Filtrering
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.OppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import java.util.UUID

interface Oppgavehåndterer {
    fun sendTilBeslutter(
        oppgaveId: Long,
        behandlendeSaksbehandler: SaksbehandlerFraApi,
    )

    fun sendIRetur(
        oppgaveId: Long,
        besluttendeSaksbehandler: SaksbehandlerFraApi,
    )

    fun endretEgenAnsattStatus(
        erEgenAnsatt: Boolean,
        fødselsnummer: String,
    )

    fun venterPåSaksbehandler(oppgaveId: Long): Boolean

    fun oppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        offset: Int,
        limit: Int,
        sortering: List<Oppgavesortering>,
        filtrering: Filtrering,
    ): OppgaverTilBehandling

    fun antallOppgaver(saksbehandlerFraApi: SaksbehandlerFraApi): AntallOppgaver

    fun behandledeOppgaver(
        saksbehandlerFraApi: SaksbehandlerFraApi,
        offset: Int,
        limit: Int,
    ): BehandledeOppgaver

    fun hentEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<Oppgaveegenskap>
}
