package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver

interface OppgaverQuerySchema : Query {
    fun antallOppgaver(env: DataFetchingEnvironment): DataFetcherResult<ApiAntallOppgaver>
}

class OppgaverQuery(
    private val handler: OppgaverQuerySchema,
) : OppgaverQuerySchema by handler
