package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.serializer.defaultGraphQLSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
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
import java.util.*

internal class SpeilSnapshotGraphQLClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
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
            "Henter nytt speil-snapshot for {}",
            StructuredArguments.keyValue("fødselsnummer", fnr)
        )
        execute(request)
    } catch (e: IOException) {
        if (retries > 0) {
            delay(retryInterval)
            execute(request, fnr, retries - 1)
        } else {
            sikkerLogg.error("Gir opp etter ${this.retries} forsøk på å hente snapshot for fødselsnummer: $fnr", e)
            throw e
        }
    } catch (e: RuntimeException) {
        sikkerLogg.error("Kunne ikke hente graphql-snapshot for $fnr", e)
        throw e
    }

    private suspend fun <T : Any> execute(request: GraphQLClientRequest<T>): GraphQLClientResponse<T> {
        val accessToken = accessTokenClient.hentAccessToken(spleisClientId)
        val callId = UUID.randomUUID().toString()

        val response = httpClient.post<String>("http://spleis-api.tbd.svc.nais.local/graphql") {
            header("Authorization", "Bearer $accessToken")
            header("callId", callId)
            contentType(ContentType.Application.Json)
            body = serializer.serialize(request)
        }

        return serializer.deserialize(response, request.responseType())
    }
}
