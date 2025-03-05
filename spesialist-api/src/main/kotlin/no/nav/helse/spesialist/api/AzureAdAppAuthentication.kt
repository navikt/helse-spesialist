package no.nav.helse.spesialist.api

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.uri

fun Application.azureAdAppAuthentication(config: AzureConfig) {
    authentication {
        jwt("oidc") {
            skipWhen { call -> call.request.uri == "/graphql/playground" }
            config.configureAuthentication(this)
        }
    }
}
