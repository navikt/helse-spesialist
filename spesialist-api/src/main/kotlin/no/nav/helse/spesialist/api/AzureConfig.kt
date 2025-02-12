package no.nav.helse.spesialist.api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import java.net.URI

data class AzureConfig(
    val clientId: String,
    val issuerlUrl: String,
    val jwkProvider: JwkProvider,
    val tokenEndpoint: String,
) {
    constructor(
        clientId: String,
        issuerUrl: String,
        jwkProviderUri: String,
        tokenEndpoint: String,
    ) : this(clientId, issuerUrl, JwkProviderBuilder(URI(jwkProviderUri).toURL()).build(), tokenEndpoint)

    fun configureAuthentication(configuration: JWTAuthenticationProvider.Config) {
        configuration.verifier(jwkProvider, issuerlUrl) {
            withAudience(clientId)
        }
        configuration.validate { credentials ->
            JWTPrincipal(credentials.payload)
        }
    }
}
