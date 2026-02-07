package no.nav.helse.spesialist.client.entraid

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.LoadingCache
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.teamLogs
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner
import java.net.ProxySelector
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class EntraIDAccessTokenGenerator(
    private val clientId: String,
    private val tokenEndpoint: String,
    private val privateJwk: String,
) : AccessTokenGenerator {
    private val httpClient = createHttpClient()

    private val loadingCache: LoadingCache<String, TokenEndpointResponse> =
        Caffeine
            .newBuilder()
            .expireAfter(
                Expiry.creating { _: String, token: TokenEndpointResponse ->
                    // Behold tokenet i cachen like lenge som expires_in i tokenet, minus to minutter,
                    // så det alltid varer minst to minutter når vi fisker det frem
                    token.expires_in.minusMinutes(2L)
                },
            ).build(CacheLoader { scope -> hentToken(scope) })

    override suspend fun hentAccessToken(scope: String): String = loadingCache.get(scope).access_token

    private fun hentToken(scope: String): TokenEndpointResponse =
        runBlocking {
            logg.info("Henter token fra Azure AD for $scope")
            val response =
                httpClient.post(tokenEndpoint) {
                    accept(ContentType.Application.Json)
                    method = HttpMethod.Post
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("client_id", clientId)
                                append("scope", scope)
                                append("grant_type", "client_credentials")
                                append(
                                    "client_assertion_type",
                                    "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                                )
                                append("client_assertion", lagAssertion())
                            },
                        ),
                    )
                }
            if (response.status != HttpStatusCode.OK) {
                teamLogs.warn("Mottok ${response.status} fra Azure AD, respons:\n${response.body<String>()}")
            }
            response.body()
        }

    private fun lagAssertion(): String {
        val privateKey = RSAKey.parse(privateJwk)
        val now = Instant.now()
        return JWT
            .create()
            .apply {
                withKeyId(privateKey.keyID)
                withSubject(clientId)
                withIssuer(clientId)
                withAudience(tokenEndpoint)
                withJWTId(UUID.randomUUID().toString())
                withIssuedAt(Date.from(now))
                withNotBefore(Date.from(now))
                withExpiresAt(Date.from(now.plus(1, ChronoUnit.HOURS)))
            }.sign(Algorithm.RSA256(null, privateKey.toRSAPrivateKey()))
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TokenEndpointResponse(
        val access_token: String,
        val expires_in: Duration,
    )

    private fun createHttpClient() =
        HttpClient(Apache5) {
            install(HttpRequestRetry) {
                retryOnExceptionIf(3) { request, throwable ->
                    logg.warn("Caught exception ${throwable.message}, for url ${request.url}")
                    true
                }
                retryIf(maxRetries) { request, response ->
                    if (response.status.value.let { it in 500..599 }) {
                        logg.warn(
                            "Retrying for statuscode ${response.status.value}, for url ${request.url}",
                        )
                        true
                    } else {
                        false
                    }
                }
            }
            engine {
                customizeClient {
                    setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                }
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Application.Json,
                    JacksonConverter(
                        jacksonObjectMapper()
                            .registerModule(JavaTimeModule()),
                    ),
                )
            }
        }
}
