package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.types.GraphQLBatchResponse
import com.expediagroup.graphql.server.types.GraphQLRequest
import com.expediagroup.graphql.server.types.GraphQLResponse
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.expediagroup.graphql.server.types.GraphQLServerResponse
import graphql.GraphQL
import graphql.GraphQLContext
import org.slf4j.LoggerFactory

class LoggingGraphQLRequestHandler(graphQL: GraphQL) : GraphQLRequestHandler(graphQL) {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override suspend fun executeRequest(
        graphQLRequest: GraphQLServerRequest,
        graphQLContext: GraphQLContext,
    ): GraphQLServerResponse {
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
        val response = super.executeRequest(graphQLRequest, graphQLContext)
        when (response) {
            is GraphQLResponse<*> -> {
                val errors = response.errors
                if (!errors.isNullOrEmpty()) sikkerLogg.warn("GraphQL-respons inneholder feil: ${errors.joinToString()}")
            }
            // Vi bruker ikke batch-operasjoner per nå
            is GraphQLBatchResponse -> Unit
        }
        return response
    }
}
