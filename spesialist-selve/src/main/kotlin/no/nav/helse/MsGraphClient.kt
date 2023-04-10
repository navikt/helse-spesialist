package no.nav.helse

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class MsGraphClient(
    private val httpClient: HttpClient,
    private val tokenClient: GraphAccessTokenClient,
    private val graphUrl: String = "https://graph.microsoft.com/v1.0"
) {
    suspend fun hentGrupper(oid: UUID) {
        val token = runBlocking { tokenClient.fetchToken() }
        val response = httpClient.get(
            "$graphUrl/users/$oid/memberOf?\$select=id,displayName"
        ) {
            bearerAuth(token.access_token)
            accept(ContentType.parse("application/json"))
        }
        sikkerlogger.info("respons fra MS Graph: ${response.bodyAsText()}")
    }

    companion object {
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }
}
