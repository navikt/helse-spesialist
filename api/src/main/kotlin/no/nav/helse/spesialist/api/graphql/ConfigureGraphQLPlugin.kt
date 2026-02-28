package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.ktor.GraphQLConfiguration
import com.expediagroup.graphql.server.ktor.KtorGraphQLRequestParser
import no.nav.helse.spesialist.api.objectMapper

fun GraphQLConfiguration.configureGraphQLPlugin(spesialistSchema: SpesialistSchema) {
    engine {
        exceptionHandler = SpesialistDataFetcherExceptionHandler()
    }
    server {
        requestParser = KtorGraphQLRequestParser(objectMapper)
        contextFactory = ContextFactory()
    }
    schema(spesialistSchema::setup)
}
