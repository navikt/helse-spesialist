package no.nav.helse.spesialist.client.entraid

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgruppehenter
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.slf4j.LoggerFactory
import java.util.UUID

class MsGraphTilgangsgruppehenter(
    private val accessTokenGenerator: AccessTokenGenerator,
    private val tilgangsgruppeUuider: TilgangsgruppeUuider,
    private val msGraphUrl: String,
) : Tilgangsgruppehenter {
    private val logg = LoggerFactory.getLogger(MsGraphTilgangsgruppehenter::class.java)
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                socketTimeout = 120_000
                connectTimeout = 1_000
                connectionRequestTimeout = 40_000
            }
        }
    private val objectMapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun hentTilgangsgrupper(saksbehandlerOid: SaksbehandlerOid): Set<Tilgangsgruppe> {
        sikkerlogg.info("Henter tilgangsgrupper for saksbehandler {}", saksbehandlerOid.value)
        val (responseStatus, responseBody) =
            runBlocking {
                val response =
                    httpClient
                        .post(msGraphUrl) {
                            url {
                                path("v1.0/users/${saksbehandlerOid.value}/checkMemberGroups")
                            }
                            bearerAuth(accessTokenGenerator.hentAccessToken("https://graph.microsoft.com/.default"))
                            accept(ContentType.Application.Json)
                            contentType(ContentType.Application.Json)
                            setBody(
                                mapOf(
                                    "groupIds" to
                                        tilgangsgruppeUuider
                                            .uuiderFor(Tilgangsgruppe.entries)
                                            .map { it.toString() },
                                ),
                            )
                        }
                response.status to response.bodyAsText()
            }

        if (!responseStatus.isSuccess()) {
            sikkerlogg.warn("Fikk kode ${responseStatus.value} fra MS Graph: $responseBody")
        }

        val grupper =
            objectMapper
                .readTree(responseBody)["value"]
                .map(JsonNode::asText)
                .map(UUID::fromString)
        logg.debug("Hentet ${grupper.size} grupper fra MS")
        return tilgangsgruppeUuider.grupperFor(grupper.toSet())
    }
}
