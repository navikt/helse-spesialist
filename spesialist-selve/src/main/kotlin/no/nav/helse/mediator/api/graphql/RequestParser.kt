package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.server.execution.GraphQLRequestParser
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import io.ktor.request.*
import no.nav.helse.objectMapper
import java.io.IOException

internal class RequestParser() : GraphQLRequestParser<ApplicationRequest> {

    override suspend fun parseRequest(request: ApplicationRequest): GraphQLServerRequest = try {
        val rawRequest = request.call.receiveText()
        objectMapper.readValue(rawRequest, GraphQLServerRequest::class.java)
    } catch (e: IOException) {
        throw IOException("Unable to parse GraphQL payload.")
    }

}
