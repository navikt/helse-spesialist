package no.nav.helse.spesialist.api.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.helse.spesialist.api.AzureConfig
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import kotlin.collections.set

class AccessTokenClient(
    private val httpClient: HttpClient,
    private val azureConfig: AzureConfig,
    private val privateJwk: String,
) {
    private val log = LoggerFactory.getLogger(AccessTokenClient::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val mutex = Mutex()

    @Volatile
    private var tokenMap = HashMap<String, AadAccessToken>()

    suspend fun hentAccessToken(scope: String): String {
        val omToMinutter = Instant.now().plusSeconds(120L)
        return mutex.withLock {
            (
                tokenMap[scope]
                    ?.takeUnless { it.expiry.isBefore(omToMinutter) }
                    ?: run {
                        log.info("Henter token fra Azure AD for $scope")

                        val response: AadAccessToken =
                            try {
                                val response =
                                    httpClient.post(azureConfig.tokenEndpoint) {
                                        accept(ContentType.Application.Json)
                                        method = HttpMethod.Post
                                        setBody(
                                            FormDataContent(
                                                Parameters.build {
                                                    append("client_id", azureConfig.clientId)
                                                    append("scope", scope)
                                                    append("grant_type", "client_credentials")
                                                    append("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                                                    append("client_assertion", lagAssertion())
                                                },
                                            ),
                                        )
                                    }
                                if (response.status != HttpStatusCode.OK) {
                                    sikkerLogg.warn("Mottok ${response.status} fra Azure AD, respons:\n${response.body<String>()}")
                                }
                                response.body()
                            } catch (e: Exception) {
                                log.warn("Klarte ikke hente nytt token fra Azure AD")
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
        return JWT.create().apply {
            withKeyId(privateKey.keyID)
            withSubject(azureConfig.clientId)
            withIssuer(azureConfig.clientId)
            withAudience(azureConfig.tokenEndpoint)
            withJWTId(UUID.randomUUID().toString())
            withIssuedAt(Date.from(now))
            withNotBefore(Date.from(now))
            withExpiresAt(Date.from(now.plus(1, ChronoUnit.HOURS)))
        }.sign(Algorithm.RSA256(null, privateKey.toRSAPrivateKey()))
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AadAccessToken(
    val access_token: String,
    val expires_in: Duration,
) {
    internal val expiry = Instant.now().plus(expires_in)
}
