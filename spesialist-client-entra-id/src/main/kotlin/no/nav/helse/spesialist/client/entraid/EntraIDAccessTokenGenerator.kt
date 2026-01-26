package no.nav.helse.spesialist.client.entraid

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.teamLogs
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
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
    private val mutex = Mutex()

    @Volatile
    private var tokenMap = HashMap<String, TokenEndpointResponse>()

    override suspend fun hentAccessToken(scope: String): String {
        val omToMinutter = Instant.now().plusSeconds(120L)
        return mutex.withLock {
            (
                tokenMap[scope]
                    ?.takeUnless { it.expiry.isBefore(omToMinutter) }
                    ?: run {
                        logg.info("Henter token fra Azure AD for $scope")

                        val response: TokenEndpointResponse =
                            try {
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
                            } catch (e: Exception) {
                                logg.warn("Klarte ikke hente nytt token fra Azure AD")
                                throw RuntimeException("Klarte ikke hente nytt token fra Azure AD", e)
                            }
                        tokenMap[scope] = response
                        response
                    }
            ).access_token
        }
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
    ) {
        val expiry: Instant = Instant.now().plus(expires_in)
    }

    private fun createHttpClient() =
        HttpClient(Apache) {
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
