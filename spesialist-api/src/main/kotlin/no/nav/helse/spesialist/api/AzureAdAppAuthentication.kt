package no.nav.helse.spesialist.api

import com.expediagroup.graphql.server.types.GraphQLRequest
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import kotlinx.coroutines.runBlocking
import no.nav.helse.bootstrap.Environment

fun Application.azureAdAppAuthentication(
    config: AzureConfig,
    env: Environment,
) {
    authentication {
        jwt("oidc") {
            skipWhen { call -> (env.erLokal || env.erDev) && gjelderIntrospection(call) || call.request.uri == "/graphql/playground" }
            config.configureAuthentication(this)
        }
    }
}

private fun gjelderIntrospection(call: ApplicationCall) = call.request.isIntrospectionRequest()

private fun ApplicationRequest.isIntrospectionRequest(): Boolean {
    if (uri != "/graphql") return false
    val body = runBlocking { call.receiveText() }
    if (body.isBlank()) return false
    val graphQLRequest = objectMapper.readValue(body, GraphQLRequest::class.java)
    return graphQLRequest.operationName == "IntrospectionQuery" ||
        graphQLRequest.query.trimStart()
            .startsWith("query IntrospectionQuery")
}
