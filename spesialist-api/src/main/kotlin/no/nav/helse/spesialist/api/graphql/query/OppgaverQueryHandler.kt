package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiFiltrering
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavesortering
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

class OppgaverQueryHandler(private val apiOppgaveService: ApiOppgaveService) : OppgaverQuerySchema {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override suspend fun behandledeOppgaverFeed(
        offset: Int,
        limit: Int,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiBehandledeOppgaver> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        val behandledeOppgaver =
            withContext(Dispatchers.IO) {
                apiOppgaveService.behandledeOppgaver(
                    saksbehandlerFraApi = saksbehandler,
                    offset = offset,
                    limit = limit,
                )
            }

        return DataFetcherResult.newResult<ApiBehandledeOppgaver>().data(behandledeOppgaver).build()
    }

    override suspend fun oppgaveFeed(
        offset: Int,
        limit: Int,
        sortering: List<ApiOppgavesortering>,
        filtrering: ApiFiltrering,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiOppgaverTilBehandling> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        sikkerLogg.debug("Henter OppgaverTilBehandling for ${saksbehandler.navn}")
        val (oppgaver, tid) =
            measureTimedValue {
                withContext(Dispatchers.IO) {
                    apiOppgaveService.oppgaver(
                        saksbehandlerFraApi = saksbehandler,
                        offset = offset,
                        limit = limit,
                        sortering = sortering,
                        filtrering = filtrering,
                    )
                }
            }
        sikkerLogg.debug("Query OppgaverTilBehandling er ferdig etter ${tid.inWholeMilliseconds} ms")
        val grense = 5000
        if (tid.inWholeMilliseconds > grense) {
            sikkerLogg.info("Det tok over $grense ms å hente oppgaver med disse filtrene: $filtrering")
        }

        return DataFetcherResult.newResult<ApiOppgaverTilBehandling>().data(oppgaver).build()
    }

    override suspend fun antallOppgaver(env: DataFetchingEnvironment): DataFetcherResult<ApiAntallOppgaver> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        sikkerLogg.info("Henter AntallOppgaver for ${saksbehandler.navn}")
        val (antallOppgaver, tid) =
            measureTimedValue {
                withContext(Dispatchers.IO) {
                    apiOppgaveService.antallOppgaver(
                        saksbehandlerFraApi = saksbehandler,
                    )
                }
            }
        sikkerLogg.debug("Query antallOppgaver er ferdig etter ${tid.inWholeMilliseconds} ms")

        return DataFetcherResult.newResult<ApiAntallOppgaver>().data(antallOppgaver).build()
    }
}
