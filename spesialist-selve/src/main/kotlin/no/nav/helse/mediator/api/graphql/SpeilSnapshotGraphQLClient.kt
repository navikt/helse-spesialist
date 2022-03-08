package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.serializer.defaultGraphQLSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.AccessTokenClient
import no.nav.helse.mediator.graphql.HentSnapshot
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.*

private data class GraphQLRequestBody(
    val query: String,
    val variables: Any?,
    val operationName: String?,
)

class SpeilSnapshotGraphQLClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val spleisUrl: URI,
    private val spleisClientId: String,
    private val retries: Int = 5,
    private val retryInterval: Long = 5000L
) {
    private companion object {
        val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
        val serializer: GraphQLClientSerializer = defaultGraphQLSerializer()
    }

    internal fun hentSnapshot(fnr: String): GraphQLClientResponse<HentSnapshot.Result> {
        val request = HentSnapshot(variables = HentSnapshot.Variables(fnr = fnr))

        return runBlocking {
            execute(request, fnr, retries)
        }
    }

    private suspend fun <T : Any> execute(
        request: GraphQLClientRequest<T>,
        fnr: String,
        retries: Int
    ): GraphQLClientResponse<T> = try {
        sikkerLogg.info(
            "Henter nytt graphql-snapshot for {}",
            StructuredArguments.keyValue("fødselsnummer", fnr)
        )
        execute(request)
    } catch (e: Exception) {
        when (e) {
            is ServerResponseException,
            is IOException -> {
                if (retries > 0) {
                    delay(retryInterval)
                    execute(request, fnr, retries - 1)
                } else {
                    sikkerLogg.error(
                        "Gir opp etter ${this.retries} forsøk på å hente graphql-snapshot for fødselsnummer: $fnr",
                        e
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

        val response = httpClient.post<String>(spleisUrl.resolve("graphql").toURL()) {
            header("Authorization", "Bearer $accessToken")
            header("callId", callId)
            contentType(ContentType.Application.Json)
            body = GraphQLRequestBody(
                query = request.query,
                variables = request.variables,
                operationName = request.operationName,
            )
        }

        val graphQLResponse = serializer.deserialize(response, request.responseType())

        if (graphQLResponse.errors !== null) {
            sikkerLogg.error("Fikk følgende graphql-feil: ${graphQLResponse.errors}")
        }

        return graphQLResponse
    }
}
