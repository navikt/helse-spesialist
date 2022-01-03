package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.serializer.defaultGraphQLSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.AccessTokenClient
import no.nav.helse.mediator.graphql.HentSnapshot
import java.util.*

internal class SpeilSnapshotGraphQLClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val spleisClientId: String,
) {
    private val serializer: GraphQLClientSerializer = defaultGraphQLSerializer()

    internal fun hentSnapshot(fnr: String): GraphQLClientResponse<HentSnapshot.Result> {
        val request = HentSnapshot(variables = HentSnapshot.Variables(fnr = fnr))

        return runBlocking {
            execute(request)
        }
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
