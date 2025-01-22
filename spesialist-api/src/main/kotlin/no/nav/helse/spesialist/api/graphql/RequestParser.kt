package no.nav.helse.spesialist.api.graphql

import com.expediagroup.graphql.server.ktor.KtorGraphQLRequestParser
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receiveText
import no.nav.helse.spesialist.api.objectMapper
import java.io.IOException

/*
    Prøvde å fjerne denne og bruke KtorGraphQLRequestParser(objectMapper) direkte, men da får vi denne feilmeldingen:

    Caused by: java.lang.NoSuchMethodError: 'java.lang.Object io.ktor.server.request.ApplicationReceiveFunctionsKt.receiveNullable(io.ktor.server.application.ApplicationCall, io.ktor.util.reflect.TypeInfo, kotlin.coroutines.Continuation)'
*/
class RequestParser : KtorGraphQLRequestParser(objectMapper) {
    override suspend fun parseRequest(request: ApplicationRequest): GraphQLServerRequest =
        try {
            val rawRequest = request.call.receiveText()
            objectMapper.readValue(rawRequest, GraphQLServerRequest::class.java)
        } catch (e: IOException) {
            throw IOException("Unable to parse GraphQL payload.")
        }
}
