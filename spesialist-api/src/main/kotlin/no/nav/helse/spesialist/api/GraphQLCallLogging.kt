package no.nav.helse.spesialist.api

import com.expediagroup.graphql.server.types.GraphQLBatchResponse
import com.expediagroup.graphql.server.types.GraphQLRequest
import com.expediagroup.graphql.server.types.GraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.expediagroup.graphql.server.types.GraphQLServerResponse
import io.ktor.content.TextContent
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.server.request.receive
import org.slf4j.LoggerFactory

private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

val GraphQLCallLogging =
    createRouteScopedPlugin("GraphQLCallLogging") {
        onCall { call ->
            val graphQLRequest = call.receive<GraphQLServerRequest>()
            logRequest(graphQLRequest)
        }
        on(ResponseBodyReadyForSend) { _, body ->
            if (body is TextContent) logResponse(objectMapper.readValue(body.text, GraphQLResponse::class.java))
        }
    }

fun logRequest(graphQLRequest: GraphQLServerRequest) {
    if (graphQLRequest is GraphQLRequest) {
        graphQLRequest.operationName.also { operationName ->
            if (operationName != null) {
                sikkerLogg.trace("GraphQL-kall mottatt, operationName: $operationName")
                if (graphQLRequest.query.startsWith("mutation") || operationName.contains(Regex("/mutation/i"))) {
                    sikkerLogg.debug("Behandler GraphQL-kall: {}", graphQLRequest)
                }
            } else if (!graphQLRequest.query.contains("query IntrospectionQuery")) {
                sikkerLogg.warn("GraphQL-kall uten navngitt query, bør fikses i kallende kode. Requesten:\n${graphQLRequest.query}")
            }
        }
    }
}

fun logResponse(response: GraphQLServerResponse) {
    when (response) {
        is GraphQLResponse<*> -> {
            val errors = response.errors
            if (!errors.isNullOrEmpty()) sikkerLogg.warn("GraphQL-respons inneholder feil: ${errors.joinToString()}")
        }
        // Vi bruker ikke batch-operasjoner per nå
        is GraphQLBatchResponse -> Unit
    }
}
