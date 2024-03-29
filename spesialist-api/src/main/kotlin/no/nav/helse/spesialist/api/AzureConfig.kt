package no.nav.helse.spesialist.api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import java.net.URI

data class AzureConfig(
    val clientId: String,
    val issuer: String,
    val jwkProvider: JwkProvider,
    val tokenEndpoint: String,
) {
    constructor(
        clientId: String,
        issuer: String,
        jwkProviderUri: String,
        tokenEndpoint: String,
    ) : this(clientId, issuer, JwkProviderBuilder(URI(jwkProviderUri).toURL()).build(), tokenEndpoint)

}
