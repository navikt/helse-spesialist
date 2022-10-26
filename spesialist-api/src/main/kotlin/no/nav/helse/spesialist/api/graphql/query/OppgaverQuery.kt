package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
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

class OppgaverQuery(private val oppgaveApiDao: OppgaveApiDao, private val oppgaveService: OppgaveService) : Query {

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
        val tilganger = env.graphQlContext.get<SaksbehandlerTilganger>("tilganger")
        val oppgaver = oppgaveApiDao.finnOppgaver(tilganger)

        return DataFetcherResult.newResult<List<OppgaveForOversiktsvisning>>().data(oppgaver).build()
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
