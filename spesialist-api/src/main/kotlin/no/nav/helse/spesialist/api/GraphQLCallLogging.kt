package no.nav.helse.spesialist.api

import com.expediagroup.graphql.server.types.GraphQLRequest
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.receive
import no.nav.helse.spesialist.application.logg.sikkerlogg

internal val GraphQLCallLogging =
    createRouteScopedPlugin("GraphQLCallLogging") {
        onCall { call ->
            val graphQLRequest = call.receive<GraphQLServerRequest>()
            logRequest(graphQLRequest)
        }
    }

private fun logRequest(graphQLRequest: GraphQLServerRequest) {
    if (graphQLRequest is GraphQLRequest) {
        graphQLRequest.operationName.also { operationName ->
            if (operationName != null) {
                sikkerlogg.trace("GraphQL-kall mottatt, operationName: $operationName")
                if (graphQLRequest.query.startsWith("mutation") || operationName.contains(Regex("/mutation/i"))) {
                    sikkerlogg.debug("Behandler GraphQL-kall: {}", graphQLRequest)
                }
            } else if (!graphQLRequest.query.contains("query IntrospectionQuery")) {
                sikkerlogg.warn("GraphQL-kall uten navngitt query, bør fikses i kallende kode. Requesten:\n${graphQLRequest.query}")
            }
        }
    }
}
