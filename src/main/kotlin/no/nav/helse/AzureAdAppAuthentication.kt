package no.nav.helse

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.Application
import io.ktor.application.log
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import java.net.URL

internal fun Application.azureAdAppAuthentication(
    oidcDiscovery: OidcDiscovery,
    config: AzureAdAppConfig,
    jwkProvider: JwkProvider = JwkProviderBuilder(URL(oidcDiscovery.jwks_uri)).build()
) {
    authentication {
        jwt(name = "saksbehandler") {
            verifier(jwkProvider, oidcDiscovery.issuer)
            validate { credentials ->
                val groupsClaim = credentials.payload.getClaim("groups").asList(String::class.java)
                if (config.requiredGroup !in groupsClaim || config.clientId !in credentials.payload.audience) {
                    log.info("${credentials.payload.subject} with audience ${credentials.payload.audience} is not authorized to use this app, denying access")
                    return@validate null
                }
                JWTPrincipal(credentials.payload)
            }
        }
    }
}

internal data class AzureAdAppConfig(
    internal val clientId: String,
    internal val requiredGroup: String
)
