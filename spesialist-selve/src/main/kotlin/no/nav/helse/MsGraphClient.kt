package no.nav.helse

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

internal class MsGraphClient(
    private val httpClient: HttpClient,
    private val tokenClient: GraphAccessTokenClient,
    private val graphUrl: String = "https://graph.microsoft.com/beta"
) {
    suspend fun hentGrupper() {
        val token = runBlocking { tokenClient.fetchToken() }

        val groupId = "a7476a04-cec2-44dd-947f-efc745f199a7"
        val response = httpClient.get(
        "$graphUrl/groups/$groupId/members?\$select=id,givenName,surname,onPremisesSamAccountName&\$top=500") {
            bearerAuth(token.access_token)
            accept(ContentType.parse("application/json"))
        }
        sikkerlogger.info("hentet medlemmer i tbd-gruppa: ${response.bodyAsText()}")
    }

    companion object {
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }
}
