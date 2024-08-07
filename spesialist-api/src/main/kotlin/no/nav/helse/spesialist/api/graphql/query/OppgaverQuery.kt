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
import java.time.Duration

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
        sikkerLogg.info("Henter OppgaverTilBehandling")
        val startTrace = startSporing(env)
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        val oppgaver =
            withContext(Dispatchers.IO) {
                oppgavehåndterer.oppgaver(
                    saksbehandlerFraApi = saksbehandler,
                    offset = offset,
                    limit = limit,
                    sortering = sortering,
                    filtrering = filtrering,
                )
            }
        avsluttSporing(startTrace)

        return DataFetcherResult.newResult<OppgaverTilBehandling>().data(oppgaver).build()
    }

    @Suppress("unused")
    suspend fun antallOppgaver(env: DataFetchingEnvironment): DataFetcherResult<AntallOppgaver> {
        sikkerLogg.info("Henter AntallOppgaver")
        val startTrace = startSporing(env)
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER)
        val antallOppgaver =
            withContext(Dispatchers.IO) {
                oppgavehåndterer.antallOppgaver(
                    saksbehandlerFraApi = saksbehandler,
                )
            }
        avsluttSporing(startTrace)

        return DataFetcherResult.newResult<AntallOppgaver>().data(antallOppgaver).build()
    }

    private fun startSporing(env: DataFetchingEnvironment): Long {
        val hvem = env.graphQlContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).navn
        sikkerLogg.trace("Henter oppgaver for $hvem")
        return System.nanoTime()
    }

    private fun avsluttSporing(start: Long) {
        val tidBrukt = Duration.ofNanos(System.nanoTime() - start)
        sikkerLogg.trace("Hentet oppgaver, det tok ${tidBrukt.toMillis()} ms")
    }
}
