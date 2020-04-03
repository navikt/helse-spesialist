package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.HttpClient
import io.ktor.client.request.get


class AzureAad(private val httpClient: HttpClient) {

    internal suspend fun oidcDiscovery(url: String): OidcDiscovery {
        return httpClient.get(url)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OidcDiscovery(val token_endpoint: String)
