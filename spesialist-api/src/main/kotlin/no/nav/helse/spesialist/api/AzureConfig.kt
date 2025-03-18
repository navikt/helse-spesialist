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
    companion object {
        fun fraEnv(env: Map<String, String>) =
            AzureConfig(
                clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
                jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
                tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            )
    }

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
