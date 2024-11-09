package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.AntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.BehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.Filtrering
import no.nav.helse.spesialist.api.graphql.schema.OppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

class OppgaverQuery(private val oppgavehåndterer: Oppgavehåndterer) : Query {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    @Suppress("unused")
    suspend fun behandledeOppgaverFeed(
        offset: Int,
        limit: Int,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<BehandledeOppgaver> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        val behandledeOppgaver =
            withContext(Dispatchers.IO) {
                oppgavehåndterer.behandledeOppgaver(
                    saksbehandlerFraApi = saksbehandler,
                    offset = offset,
                    limit = limit,
                )
            }

        return DataFetcherResult.newResult<BehandledeOppgaver>().data(behandledeOppgaver).build()
    }

    @Suppress("unused")
    suspend fun oppgaveFeed(
        offset: Int,
        limit: Int,
        sortering: List<Oppgavesortering>,
        filtrering: Filtrering,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<OppgaverTilBehandling> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        sikkerLogg.debug("Henter OppgaverTilBehandling for ${saksbehandler.navn}")
        val (oppgaver, tid) =
            measureTimedValue {
                withContext(Dispatchers.IO) {
                    oppgavehåndterer.oppgaver(
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

        return DataFetcherResult.newResult<OppgaverTilBehandling>().data(oppgaver).build()
    }

    @Suppress("unused")
    suspend fun antallOppgaver(env: DataFetchingEnvironment): DataFetcherResult<AntallOppgaver> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        sikkerLogg.info("Henter AntallOppgaver for ${saksbehandler.navn}")
        val (antallOppgaver, tid) =
            measureTimedValue {
                withContext(Dispatchers.IO) {
                    oppgavehåndterer.antallOppgaver(
                        saksbehandlerFraApi = saksbehandler,
                    )
                }
            }
        sikkerLogg.debug("Query antallOppgaver er ferdig etter ${tid.inWholeMilliseconds} ms")

        return DataFetcherResult.newResult<AntallOppgaver>().data(antallOppgaver).build()
    }
}
