package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson

interface PersonQuerySchema : Query {
    fun person(
        personPseudoId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPerson?>
}

class PersonQuery(
    private val handler: PersonQuerySchema,
) : PersonQuerySchema by handler
