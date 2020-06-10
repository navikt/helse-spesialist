package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.HttpClient
import io.ktor.client.request.get


class AzureAadClient(private val httpClient: HttpClient) {
    internal suspend fun oidcDiscovery(url: String): OidcDiscovery = httpClient.get(url)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OidcDiscovery(val token_endpoint: String, val jwks_uri: String, val issuer: String)
