package no.nav.helse

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.Gruppekontroll
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.api.client.AccessTokenClient
import org.slf4j.LoggerFactory

class MsGraphClient(
    private val httpClient: HttpClient,
    private val tokenClient: AccessTokenClient,
    private val graphUrl: String = "https://graph.microsoft.com/v1.0",
): Gruppekontroll {

    override suspend fun erIGrupper(oid: UUID, gruppeIder: List<UUID>): Boolean {
        val token = tokenClient.hentAccessToken("https://graph.microsoft.com/.default")
        val response = httpClient.post(graphUrl) {
            url {
                path("v1.0/users/$oid/checkMemberGroups")
            }
            bearerAuth(token)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(mapOf("groupIds" to gruppeIder.map { it.toString() }))
        }

        val responseNode = objectMapper.readTree(response.bodyAsText())
        val grupper = responseNode["value"].map { it.asUUID() }

        val harTilgang = grupper.containsAll(gruppeIder)
        if (harTilgang) {
            sikkerlogger.info("{} er medlem av $gruppeIder", kv("oid", oid))
        } else {
            sikkerlogger.info("{} mangler tilgang til minst en av $gruppeIder", kv("oid", oid))
        }
        return harTilgang
    }

    private companion object {
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }
}
