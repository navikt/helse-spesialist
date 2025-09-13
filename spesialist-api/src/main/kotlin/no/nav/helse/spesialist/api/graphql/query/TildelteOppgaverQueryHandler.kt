package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.notFound
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiSaksbehandler
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Saksbehandler
import kotlin.time.measureTimedValue

class TildelteOppgaverQueryHandler(
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerDao: SaksbehandlerDao,
) : TildelteOppgaverQuerySchema {
    override suspend fun tildelteOppgaverFeed(
        offset: Int,
        limit: Int,
        oppslattSaksbehandler: ApiSaksbehandler,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiOppgaverTilBehandling> {
        val innloggetSaksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER)

        val oppslåttSaksbehandler =
            oppslattSaksbehandler.ident
                ?.let { saksbehandlerDao.hent(it) ?: return notFound("Finner ikke saksbehandler.") }
                ?: return notFound("Saksbehandler mangler ident.")

        sikkerlogg.info(
            "${innloggetSaksbehandler.navn} (${innloggetSaksbehandler.ident}) søker opp tildelte oppgaver for ${oppslåttSaksbehandler.navn} (${oppslåttSaksbehandler.ident})",
        )
        val (oppgaver, tid) =
            measureTimedValue {
                apiOppgaveService.tildelteOppgaver(
                    innloggetSaksbehandler = innloggetSaksbehandler,
                    tilgangsgrupper = env.graphQlContext.get(ContextValues.TILGANGSGRUPPER),
                    oppslåttSaksbehandler = oppslåttSaksbehandler,
                    offset = offset,
                    limit = limit,
                )
            }
        sikkerlogg.debug("Query tildelteOppgaver er ferdig etter ${tid.inWholeMilliseconds} ms")
        return byggRespons(oppgaver)
    }
}
