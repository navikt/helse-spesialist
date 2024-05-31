package no.nav.helse.spesialist.api

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt

fun Application.azureAdAppAuthentication(config: AzureConfig) {
    authentication {
        jwt("oidc") {
            config.configureAuthentication(this)
        }
    }
}
