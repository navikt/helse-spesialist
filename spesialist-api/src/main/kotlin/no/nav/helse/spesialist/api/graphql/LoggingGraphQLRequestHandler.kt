package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.types.GraphQLRequest
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.expediagroup.graphql.server.types.GraphQLServerResponse
import graphql.GraphQL
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggingGraphQLRequestHandler(graphQL: GraphQL) : GraphQLRequestHandler(graphQL) {

    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    @Suppress("deprecation")
    override suspend fun executeRequest(
        graphQLRequest: GraphQLServerRequest,
        context: com.expediagroup.graphql.generator.execution.GraphQLContext?,
        graphQLContext: Map<*, Any>,
    ): GraphQLServerResponse {
        if (graphQLRequest is GraphQLRequest) {
            graphQLRequest.operationName.let {
                if (it != null) sikkerLogg.trace("GraphQL-kall mottatt, operationName: $it")
                else sikkerLogg.warn("GraphQL-kall uten navngitt query, bør fikses i kallende kode. Requesten:\n${graphQLRequest.query}")
            }
        }
        return super.executeRequest(graphQLRequest, context, graphQLContext)
    }
}
