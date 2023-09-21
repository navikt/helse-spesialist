package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.types.GraphQLRequest
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.expediagroup.graphql.server.types.GraphQLServerResponse
import graphql.GraphQL
import graphql.GraphQLContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggingGraphQLRequestHandler(graphQL: GraphQL) : GraphQLRequestHandler(graphQL) {

    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override suspend fun executeRequest(
        graphQLRequest: GraphQLServerRequest,
        graphQLContext: GraphQLContext
    ): GraphQLServerResponse {
        if (graphQLRequest is GraphQLRequest) {
            graphQLRequest.operationName.let {
                if (it != null) sikkerLogg.trace("GraphQL-kall mottatt, operationName: $it")
                else sikkerLogg.warn("GraphQL-kall uten navngitt query, bør fikses i kallende kode. Requesten:\n${graphQLRequest.query}")
            }
        }
        return super.executeRequest(graphQLRequest, graphQLContext)
    }
}
