package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.graphql.schema.FerdigstiltOppgave
import no.nav.helse.spesialist.api.graphql.schema.OppgaveForOversiktsvisning
import no.nav.helse.spesialist.api.graphql.schema.Oppgaver
import no.nav.helse.spesialist.api.graphql.schema.Sortering
import no.nav.helse.spesialist.api.graphql.schema.tilFerdigstilteOppgaver
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.experimental.OppgaveService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppgaverQuery(private val oppgaveApiDao: OppgaveApiDao, private val oppgaveService: OppgaveService) : Query {

    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    @Suppress("unused")
    fun behandledeOppgaver(
        behandletAvIdent: String,
        behandletAvOid: String,
        fom: String?
    ): DataFetcherResult<List<FerdigstiltOppgave>> {
        val fraOgMed = try {
            LocalDate.parse(fom)
        } catch (_: Exception) {
            null
        }

        val oppgaver =
            oppgaveApiDao.hentBehandledeOppgaver(behandletAvIdent, UUID.fromString(behandletAvOid), fraOgMed)
                .tilFerdigstilteOppgaver()

        return DataFetcherResult.newResult<List<FerdigstiltOppgave>>().data(oppgaver).build()
    }

    @Suppress("unused")
    fun alleOppgaver(env: DataFetchingEnvironment): DataFetcherResult<List<OppgaveForOversiktsvisning>> {
        val start = startSporing(env)
        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>("tilganger")
        val oppgaver = oppgaveApiDao.finnOppgaver(tilganger)
        avsluttSporing(start)

        return DataFetcherResult.newResult<List<OppgaveForOversiktsvisning>>().data(oppgaver).build()
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
    @Suppress("unused")
    fun oppgaver(
        antall: Int,
        side: Int,
        sortering: Sortering? = null,
        env: DataFetchingEnvironment
    ): DataFetcherResult<Oppgaver> {
        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>("tilganger")
        val paginerteOppgaver = oppgaveService.hentOppgaver(
            tilganger = tilganger,
            antall = antall,
            side = side,
            sortering = sortering
        )

        return DataFetcherResult.newResult<Oppgaver>().data(paginerteOppgaver).build()
    }

}
