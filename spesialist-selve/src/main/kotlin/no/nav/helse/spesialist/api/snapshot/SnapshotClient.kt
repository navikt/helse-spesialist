package no.nav.helse.spesialist.api.snapshot

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.spesialist.api.client.AccessTokenClient
import no.nav.helse.spleis.graphql.HentSnapshot
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.UUID

interface ISnapshotClient {
    fun hentSnapshot(fnr: String): GraphQLClientResponse<HentSnapshot.Result>
}

class SnapshotClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val spleisUrl: URI,
    private val spleisClientId: String,
    private val retries: Int = 5,
    private val retryInterval: Long = 5000L,
) : ISnapshotClient {
    private companion object {
        val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
        val logg: Logger = LoggerFactory.getLogger(SnapshotClient::class.java)
        val serializer: GraphQLClientSerializer = GraphQLClientJacksonSerializer(jacksonObjectMapper().disable(FAIL_ON_UNKNOWN_PROPERTIES))
    }

    override fun hentSnapshot(fnr: String): GraphQLClientResponse<HentSnapshot.Result> {
        val request = HentSnapshot(variables = HentSnapshot.Variables(fnr = fnr))

        return runBlocking {
            execute(request, fnr, retries)
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
        val accessToken = accessTokenClient.hentAccessToken(spleisClientId)
        val callId = UUID.randomUUID().toString()

        val response =
            httpClient
                .post(spleisUrl.resolve("/graphql").toURL()) {
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
                }.body<String>()

        val graphQLResponse = serializer.deserialize(response, request.responseType())

        if (graphQLResponse.errors !== null) {
            logg.error("Feil i graphQL-response. Se sikkerlogg for mer info")
            sikkerLogg.error("Fikk følgende graphql-feil: ${graphQLResponse.errors}")
        }

        return graphQLResponse
    }
}

private data class GraphQLRequestBody(
    val query: String,
    val variables: Any?,
    val operationName: String?,
)
