package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.rest.saksbilde.ApiSaksbildePerson

interface PersonQuerySchema : Query {
    fun person(
        personPseudoId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiSaksbildePerson?>
}

class PersonQuery(
    private val handler: PersonQuerySchema,
) : PersonQuerySchema by handler
