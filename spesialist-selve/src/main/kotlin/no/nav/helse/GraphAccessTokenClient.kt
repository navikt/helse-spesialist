package no.nav.helse

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.slf4j.LoggerFactory

internal val logger = LoggerFactory.getLogger("MsGraphClient")

internal class GraphAccessTokenClient(
    val httpClient: HttpClient,
    val azureConfig: AzureConfig,
    val privateJwk: String,
) {
    internal suspend fun fetchToken(): AadAccessToken {
        val privateKey = RSAKey.parse(privateJwk)
        val now = Instant.now()
        val clientAssertion = JWT.create().apply {
            withKeyId(privateKey.keyID)
            withSubject(azureConfig.clientId)
            withIssuer(azureConfig.clientId)
            withAudience(azureConfig.tokenEndpoint)
            withJWTId(UUID.randomUUID().toString())
            withIssuedAt(Date.from(now))
            withNotBefore(Date.from(now))
            withExpiresAt(Date.from(now.plusSeconds(120)))
        }.sign(Algorithm.RSA256(null, privateKey.toRSAPrivateKey()))

        val callId = UUID.randomUUID()
        logger.info("Henter Azure token for MS graph")
        val token: AadAccessToken = httpClient.preparePost(azureConfig.tokenEndpoint) {
            header("callId", callId)
            contentType(ContentType.Application.Json)
            setBody(FormDataContent(Parameters.build {
                append("grant_type", "client_credentials")
                append("scope", "https://graph.microsoft.com/.default")
                append("client_id", azureConfig.clientId)
                append("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                append("client_assertion", clientAssertion)
            }))
        }.body()

        logger.info("hentet token for MS graph: $token")

        return token
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    internal data class AadAccessToken(
        val access_token: String,
    )
}
