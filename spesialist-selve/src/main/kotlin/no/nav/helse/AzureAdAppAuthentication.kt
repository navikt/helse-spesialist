package no.nav.helse

import com.auth0.jwk.JwkProvider
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*

internal fun Application.azureAdAppAuthentication(config: AzureAdAppConfig) {
    authentication {
        jwt("oidc") {
            config.configureAuthentication(this)
        }
    }
}

internal class AzureAdAppConfig(
    private val clientId: String,
    private val issuer: String,
    private val jwkProvider: JwkProvider
) {
    fun configureAuthentication(configuration: JWTAuthenticationProvider.Configuration) {
        configuration.verifier(jwkProvider, issuer) {
            withAudience(clientId)
        }
        configuration.validate { credentials -> JWTPrincipal(credentials.payload) }
    }
}
