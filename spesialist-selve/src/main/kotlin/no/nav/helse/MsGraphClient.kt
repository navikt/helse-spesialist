package no.nav.helse

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.path
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class MsGraphClient(
    private val httpClient: HttpClient,
    private val tokenClient: GraphAccessTokenClient,
    private val graphUrl: String = "https://graph.microsoft.com/v1.0",
) {
    suspend fun hentGrupper(oid: UUID) {
        val token = runBlocking { tokenClient.fetchToken() }
        val groupId = "a7476a04-cec2-44dd-947f-efc745f199a7"
        val response = httpClient.get(graphUrl) {
            url {
                path("v1.0/groups/$groupId/members")
                parameters.append("\$filter", "id eq '$oid'")
                parameters.append("\$count", "true")
            }
            bearerAuth(token.access_token)
            accept(ContentType.parse("application/json"))
            header("ConsistencyLevel", "eventual")
        }
        val responseText = response.bodyAsText()
        sikkerlogger.info("respons fra MS Graph: $responseText")
        val responseNode = objectMapper.readTree(responseText)
        responseNode["value"].firstOrNull()?.path("displayName")?.let {
            sikkerlogger.info("$it er medlem av $groupId")
        } ?: sikkerlogger.info("$oid er ikke medlem av $groupId")

    }

    companion object {
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }
}
