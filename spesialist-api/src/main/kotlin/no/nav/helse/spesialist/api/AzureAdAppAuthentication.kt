package no.nav.helse.spesialist.api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.uri
import java.net.URI

fun Application.azureAdAppAuthentication(config: ApiModule.Configuration) {
    azureAdAppAuthentication(
        jwkProvider = JwkProviderBuilder(URI(config.jwkProviderUri).toURL()).build(),
        issuerUrl = config.issuerUrl,
        clientId = config.clientId,
    )
}

fun Application.azureAdAppAuthentication(
    jwkProvider: JwkProvider,
    issuerUrl: String,
    clientId: String,
) {
    authentication {
        jwt("oidc") {
            skipWhen { call -> call.request.uri == "/graphql/playground" }
            verifier(jwkProvider, issuerUrl) {
                withAudience(clientId)
            }
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}
