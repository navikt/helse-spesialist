package no.nav.helse.spesialist.client.spleis

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import java.net.URI
import java.util.UUID.randomUUID

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

    private val httpClient =
        HttpClient(Apache5) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                socketTimeout = 120_000
                connectTimeout = 1_000
                connectionRequestTimeout = 40_000
            }
        }

    fun hentPerson(fødselsnummer: String): GraphQLPerson? {
        val request = HentSnapshot(variables = HentSnapshot.Variables(fnr = fødselsnummer))
        val accessToken = accessTokenGenerator.hentAccessToken(spleisClientId)
        val callId = randomUUID().toString()

        return runBlocking {
            val response =
                httpClient
                    .post(spleisUrl.resolve("/graphql").toURL()) {
                        header("Authorization", "Bearer $accessToken")
                        header("callId", callId)
                        contentType(ContentType.Application.Json)
                        setBody(
                            GraphQLRequestBody(
                                query = request.query,
                                variables = request.variables,
                                operationName = request.operationName,
                            ),
                        )
                    }
            val responseBody = response.body<String>()
            if (loggRespons) {
                teamLogs.trace("Fikk HTTP ${response.status.value}-svar fra Spleis: $responseBody")
            }
            if (!response.status.isSuccess()) {
                logg.error("Fikk HTTP ${response.status.value} i svar fra Spleis. Se sikkerlogg for mer info.")
                teamLogs.error("Fikk HTTP ${response.status.value}-svar fra Spleis: $responseBody")
            }
            val graphQLResponse = serializer.deserialize(responseBody, request.responseType())
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

    private data class GraphQLRequestBody(
        val query: String,
        val variables: HentSnapshot.Variables,
        val operationName: String,
    )
}
