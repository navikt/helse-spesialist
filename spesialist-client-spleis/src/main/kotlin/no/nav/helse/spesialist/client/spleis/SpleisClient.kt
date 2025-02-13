package no.nav.helse.spesialist.client.spleis

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.UUID

class SpleisClient(
    private val httpClient: HttpClient,
    private val accessTokenGenerator: AccessTokenGenerator,
    private val spleisUrl: URI,
    private val spleisClientId: String,
    private val retries: Int = 5,
    private val retryInterval: Long = 5000L,
) {
    private companion object {
        val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
        val logg: Logger = LoggerFactory.getLogger(SpleisClientSnapshothenter::class.java)
        val serializer: GraphQLClientSerializer =
            GraphQLClientJacksonSerializer(jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
    }

    fun hentPerson(fødselsnummer: String): GraphQLPerson? {
        val request = HentSnapshot(variables = HentSnapshot.Variables(fnr = fødselsnummer))

        return runBlocking {
            execute(request, fødselsnummer, retries).data?.person
        }
    }

    private suspend fun <T : Any> execute(
        request: GraphQLClientRequest<T>,
        fnr: String,
        retries: Int,
    ): GraphQLClientResponse<T> =
        try {
            sikkerLogg.info(
                "Henter nytt graphql-snapshot for {}",
                StructuredArguments.keyValue("fødselsnummer", fnr),
            )
            execute(request)
        } catch (e: Exception) {
            when (e) {
                is ServerResponseException,
                is IOException,
                -> {
                    if (retries > 0) {
                        delay(retryInterval)
                        execute(request, fnr, retries - 1)
                    } else {
                        sikkerLogg.error(
                            "Gir opp etter ${this.retries} forsøk på å hente graphql-snapshot for fødselsnummer: $fnr",
                            e,
                        )
                        throw e
                    }
                }

                else -> {
                    sikkerLogg.error("Kunne ikke hente graphql-snapshot for $fnr", e)
                    throw e
                }
            }
        }

    private suspend fun <T : Any> execute(request: GraphQLClientRequest<T>): GraphQLClientResponse<T> {
        val accessToken = accessTokenGenerator.hentAccessToken(spleisClientId)
        val callId = UUID.randomUUID().toString()

        val response =
            httpClient
                .post(spleisUrl.resolve("/resources/graphql").toURL()) {
                    header("Authorization", "Bearer $accessToken")
                    header("callId", callId)
                    contentType(ContentType.Application.Json)
                    setBody(
                        request.query?.let {
                            GraphQLRequestBody(
                                query = it,
                                variables = request.variables,
                                operationName = request.operationName,
                            )
                        },
                    )
                }
        val responseBody = response.body<String>()
        if (!response.status.isSuccess()) {
            logg.error("Fikk HTTP ${response.status.value} i svar fra Spleis. Se sikkerlogg for mer info.")
            sikkerLogg.error("Fikk HTTP ${response.status.value}-svar fra Spleis: $responseBody")
        }

        val graphQLResponse = serializer.deserialize(responseBody, request.responseType())

        if (graphQLResponse.data == null && graphQLResponse.errors == null) {
            logg.error("GraphQL-svar fra Spleis manglet både data og feil. Se sikkerlogg for mer info.")
            sikkerLogg.error("Fikk GraphQL-svar fra Spleis som manglet både data og feil: $responseBody")
        }

        if (graphQLResponse.errors !== null) {
            logg.error("Feil i GraphQL-response. Se sikkerlogg for mer info")
            sikkerLogg.error("Fikk følgende graphql-feil: ${graphQLResponse.errors}")
        }

        return graphQLResponse
    }

    private data class GraphQLRequestBody(
        val query: String,
        val variables: Any?,
        val operationName: String?,
    )
}
