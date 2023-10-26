package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.schema.AntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.BehandletOppgave
import no.nav.helse.spesialist.api.graphql.schema.Fane
import no.nav.helse.spesialist.api.graphql.schema.Filtrering
import no.nav.helse.spesialist.api.graphql.schema.OppgaveTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgaveegenskap
import no.nav.helse.spesialist.api.graphql.schema.OppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.Oppgavesortering
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppgaverQuery(private val oppgavehåndterer: Oppgavehåndterer) : Query {

    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    @Suppress("unused")
    suspend fun behandledeOppgaverIDag(
        env: DataFetchingEnvironment,
    ): DataFetcherResult<List<BehandletOppgave>> {
        val saksbehandler = env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(SAKSBEHANDLER.key).value
        val behandledeOppgaver = withContext(Dispatchers.IO) {
            oppgavehåndterer.behandledeOppgaver(saksbehandler)
        }

        return DataFetcherResult.newResult<List<BehandletOppgave>>().data(behandledeOppgaver).build()
    }

    suspend fun oppgaver(
        startIndex: Int? = 0,
        pageSize: Int? = null,
        sortering: List<Oppgavesortering>? = emptyList(),
        filtrerteEgenskaper: List<Oppgaveegenskap>? = emptyList(),
        fane: Fane? = Fane.TIL_GODKJENNING,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<List<OppgaveTilBehandling>> {
        sikkerLogg.info("Henter OppgaverTilBehandling")
        val startTrace = startSporing(env)
        val saksbehandler = env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(SAKSBEHANDLER.key).value
        val oppgaver = withContext(Dispatchers.IO) {
            oppgavehåndterer.oppgaver(
                saksbehandlerFraApi = saksbehandler,
                offset = startIndex ?: 0,
                limit = pageSize ?: Int.MAX_VALUE,
                sortering = sortering ?: emptyList(),
                filtrering = Filtrering(
                    egenskaper = filtrerteEgenskaper ?: emptyList(),
                    egneSakerPaVent = fane == Fane.PAA_VENT,
                    egneSaker = fane == Fane.MINE_SAKER,
                ),
            )
        }
        avsluttSporing(startTrace)

        return DataFetcherResult.newResult<List<OppgaveTilBehandling>>().data(oppgaver.oppgaver).build()
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
        val saksbehandler = env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(SAKSBEHANDLER.key).value
        val oppgaver = withContext(Dispatchers.IO) {
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
    suspend fun antallOppgaver(env: DataFetchingEnvironment, ): DataFetcherResult<AntallOppgaver> {
        sikkerLogg.info("Henter AntallOppgaver")
        val startTrace = startSporing(env)
        val saksbehandler = env.graphQlContext.get<Lazy<SaksbehandlerFraApi>>(SAKSBEHANDLER.key).value
        val antallOppgaver = withContext(Dispatchers.IO) {
            oppgavehåndterer.antallOppgaver(
                saksbehandlerFraApi = saksbehandler,
            )
        }
        avsluttSporing(startTrace)

        return DataFetcherResult.newResult<AntallOppgaver>().data(antallOppgaver).build()
    }

    private fun startSporing(env: DataFetchingEnvironment): Long {
        val hvem = env.graphQlContext.get<String>("saksbehandlerNavn")
        sikkerLogg.trace("Henter oppgaver for $hvem")
        return System.nanoTime()
    }

    private fun avsluttSporing(start: Long) {
        val tidBrukt = Duration.ofNanos(System.nanoTime() - start)
        sikkerLogg.trace("Hentet oppgaver, det tok ${tidBrukt.toMillis()} ms")
    }
}
