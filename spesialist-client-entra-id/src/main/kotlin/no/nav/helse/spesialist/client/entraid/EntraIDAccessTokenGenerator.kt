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
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicNameValuePair
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

    override fun hentAccessToken(scope: String): String = loadingCache.get(scope).access_token

    private fun hentToken(scope: String): TokenEndpointResponse {
        loggInfo("Henter token fra Entra ID for scope $scope")

        return Request
            .post(tokenEndpoint)
            .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .bodyForm(
                BasicNameValuePair("client_id", clientId),
                BasicNameValuePair("scope", scope),
                BasicNameValuePair("grant_type", "client_credentials"),
                BasicNameValuePair("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"),
                BasicNameValuePair("client_assertion", lagAssertion()),
            ).execute()
            .handleResponse { response ->
                val responseBody = EntityUtils.toString(response.entity)
                if (response.code !in 200..299) {
                    loggError("Fikk HTTP ${response.code} fra Entra ID", "response" to responseBody)
                    error("Fikk HTTP ${response.code} fra Entra ID")
                }
                objectMapper.readValue(responseBody, TokenEndpointResponse::class.java)
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

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TokenEndpointResponse(
        val access_token: String,
        val expires_in: Duration,
    )
}
