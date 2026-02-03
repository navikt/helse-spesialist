package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.types.GraphQLRequest
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.receive
import no.nav.helse.spesialist.application.logg.teamLogs

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
                teamLogs.trace("GraphQL-kall mottatt, operationName: $operationName")
                if (graphQLRequest.query.startsWith("mutation") || operationName.contains(Regex("/mutation/i"))) {
                    teamLogs.debug("Behandler GraphQL-kall: {}", graphQLRequest)
                }
            } else if (!graphQLRequest.query.contains("query IntrospectionQuery")) {
                teamLogs.warn("GraphQL-kall uten navngitt query, bør fikses i kallende kode. Requesten:\n${graphQLRequest.query}")
            }
        }
    }
}
