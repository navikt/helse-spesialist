package no.nav.helse

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

internal fun Application.azureAdAppAuthentication(config: AzureAdAppConfig) {
    authentication {
        jwt("oidc") {
            config.configureAuthentication(this)
        }
    }
}

internal class AzureAdAppConfig(
    private val azureConfig: AzureConfig,
) {
    fun configureAuthentication(configuration: JWTAuthenticationProvider.Config) {
        configuration.verifier(azureConfig.jwkProvider, azureConfig.issuer) {
            withAudience(azureConfig.clientId)
        }
        configuration.validate { credentials -> JWTPrincipal(credentials.payload) }
    }
}
