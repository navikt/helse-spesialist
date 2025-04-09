package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiFiltrering
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaverTilBehandling
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgavesortering
import java.time.LocalDate

interface OppgaverQuerySchema : Query {
    suspend fun behandledeOppgaverFeed(
        offset: Int,
        limit: Int,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiBehandledeOppgaver>

    suspend fun behandledeOppgaverFeedV2(
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiBehandledeOppgaver>

    suspend fun oppgaveFeed(
        offset: Int,
        limit: Int,
        sortering: List<ApiOppgavesortering>,
        filtrering: ApiFiltrering,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiOppgaverTilBehandling>

    suspend fun antallOppgaver(env: DataFetchingEnvironment): DataFetcherResult<ApiAntallOppgaver>
}

class OppgaverQuery(private val handler: OppgaverQuerySchema) : OppgaverQuerySchema by handler
