package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandlerMedOid
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.application.logg.sikkerlogg
import kotlin.time.measureTimedValue

class TildelteOppgaverQueryHandler(
    private val apiOppgaveService: ApiOppgaveService,
) : TildelteOppgaverQuerySchema {
    override suspend fun tildelteOppgaverFeed(
        offset: Int,
        limit: Int,
        oppslattSaksbehandler: ApiSaksbehandlerMedOid,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiOppgaverTilBehandling> {
        val innloggetSaksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        sikkerlogg.debug(
            "${innloggetSaksbehandler.navn} (${innloggetSaksbehandler.ident}) søker opp tildelte oppgaver for ${oppslattSaksbehandler.navn} (${oppslattSaksbehandler.ident})",
        )
        val (oppgaver, tid) =
            measureTimedValue {
                apiOppgaveService.tildelteOppgaver(
                    innloggetSaksbehandlerMedOid = innloggetSaksbehandler,
                    oppslåttSaksbehandlerMedOid = oppslattSaksbehandler,
                    offset = offset,
                    limit = limit,
                )
            }
        sikkerlogg.debug("Query tildelteOppgaver er ferdig etter ${tid.inWholeMilliseconds} ms")
        return byggRespons(oppgaver)
    }
}
