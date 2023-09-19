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
import no.nav.helse.mediator.Gruppekontroll
import no.nav.helse.spesialist.api.client.AccessTokenClient
import org.slf4j.LoggerFactory

class MsGraphClient(
    private val httpClient: HttpClient,
    private val tokenClient: AccessTokenClient,
    private val graphUrl: String = "https://graph.microsoft.com/v1.0",
): Gruppekontroll {
    override suspend fun erIGruppe(oid: UUID, groupId: UUID): Boolean {
        val token = tokenClient.hentAccessToken("https://graph.microsoft.com/.default")
        val response = httpClient.get(graphUrl) {
            url {
                path("v1.0/groups/$groupId/members")
                parameters.append("\$filter", "id eq '$oid'")
                parameters.append("\$count", "true")
            }
            bearerAuth(token)
            accept(ContentType.parse("application/json"))
            header("ConsistencyLevel", "eventual")
        }

        val responseNode = objectMapper.readTree(response.bodyAsText())
        return (responseNode["@odata.count"].asText() == "1").also { harTilgang ->
            if (harTilgang) {
                val hvem = responseNode["value"].first().path("displayName").asText()
                sikkerlogger.info("$hvem er medlem av $groupId")
            } else {
                sikkerlogger.info("$oid er ikke medlem av $groupId")
            }
        }
    }

    private companion object {
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }
}
