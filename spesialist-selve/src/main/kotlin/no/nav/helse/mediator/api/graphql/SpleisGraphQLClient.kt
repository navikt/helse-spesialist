package no.nav.helse.mediator.api.graphql

import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.serializer.defaultGraphQLSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.mediator.graphql.HentEldreGenerasjoner

internal class SpleisGraphQLClient(
    private val httpClient: HttpClient,
    private val url: String = "https://spleis-api.dev-fss-pub.nais.io/graphql",
    private val serializer: GraphQLClientSerializer = defaultGraphQLSerializer()
) {
    internal suspend fun hentEldreGenerasjoner(fnr: String): GraphQLClientResponse<HentEldreGenerasjoner.Result> {
        val request = HentEldreGenerasjoner(variables = HentEldreGenerasjoner.Variables(fnr = fnr))
        return execute(request)
    }

    private suspend fun <T : Any> execute(request: GraphQLClientRequest<T>): GraphQLClientResponse<T> {
        val response = httpClient.post<String>(url) {
            contentType(ContentType.Application.Json)
            body = serializer.serialize(request)
        }
        return serializer.deserialize(response, request.responseType())
    }
}
