package no.nav.helse.spesialist.client.spleis

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spleis.graphql.HentSnapshotResult
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.util.TimeValue
import java.net.URI
import java.util.UUID

class SpleisClient(
    private val accessTokenGenerator: AccessTokenGenerator,
    private val spleisUrl: URI,
    private val spleisClientId: String,
    private val loggRespons: Boolean,
) {
    private val serializer: GraphQLClientSerializer =
        GraphQLClientJacksonSerializer(
            jacksonObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES),
        )

    class SpleisRetryStrategy : DefaultHttpRequestRetryStrategy(5, TimeValue.ofSeconds(1L)) {
        override fun handleAsIdempotent(request: HttpRequest) = true // Retry selv om det er POST
    }

    private val retryStrategy = SpleisRetryStrategy()

    fun hentPerson(fødselsnummer: String): GraphQLPerson? =
        HttpClientBuilder.create().setRetryStrategy(retryStrategy).build().use { client ->
            val callId = UUID.randomUUID().toString()
            val uri = spleisUrl.resolve("/graphql")
            val requestBody = """{ "variables": { "fnr": "$fødselsnummer" } }"""
            loggInfo("Kaller HTTP POST $uri med callId $callId", "requestBody" to requestBody)
            Request
                .post(uri)
                .setHeader("Authorization", "Bearer ${accessTokenGenerator.hentAccessToken(spleisClientId)}")
                .setHeader("callId", callId)
                .bodyString(requestBody, ContentType.APPLICATION_JSON)
                .execute(client)
                .handleResponse { response ->
                    val responseBody = EntityUtils.toString(response.entity)
                    if (loggRespons) {
                        teamLogs.trace("Fikk HTTP ${response.code}-svar fra Spleis: $responseBody")
                    }
                    if (response.code !in 200..299) {
                        logg.error("Fikk HTTP ${response.code} i svar fra Spleis. Se sikkerlogg for mer info.")
                        teamLogs.error("Fikk HTTP ${response.code}-svar fra Spleis: $responseBody")
                    }
                    val graphQLResponse = serializer.deserialize(responseBody, HentSnapshotResult::class)
                    if (graphQLResponse.data == null && graphQLResponse.errors == null) {
                        logg.error("GraphQL-svar fra Spleis manglet både data og feil. Se sikkerlogg for mer info.")
                        teamLogs.error("Fikk GraphQL-svar fra Spleis som manglet både data og feil: $responseBody")
                    }
                    if (graphQLResponse.errors !== null) {
                        logg.error("Feil i GraphQL-response. Se sikkerlogg for mer info")
                        teamLogs.error("Fikk følgende graphql-feil: ${graphQLResponse.errors}")
                    }

                    graphQLResponse.data?.person
                }
        }
}
