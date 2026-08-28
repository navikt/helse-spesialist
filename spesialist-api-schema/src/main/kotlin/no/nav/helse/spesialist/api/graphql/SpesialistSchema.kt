package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.ktor.GraphQLConfiguration
import no.nav.helse.spesialist.api.graphql.query.PersonQuery
import no.nav.helse.spesialist.api.graphql.query.PersonQuerySchema

class SpesialistSchema(
    val queryHandlers: QueryHandlers,
) {
    class QueryHandlers(
        val person: PersonQuerySchema,
    )

    fun setup(schemaConfiguration: GraphQLConfiguration.SchemaConfiguration) {
        schemaConfiguration.packages = listOf("no.nav.helse.spesialist.api.graphql")

        schemaConfiguration.queries =
            listOf(
                PersonQuery(handler = queryHandlers.person),
            )

        schemaConfiguration.hooks = schemaGeneratorHooks
    }
}
