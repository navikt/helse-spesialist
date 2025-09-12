package no.nav.helse.spesialist.client.entraid

import com.fasterxml.jackson.databind.DeserializationFeature
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
import io.ktor.http.path
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgruppehenter
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgrupper
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.slf4j.LoggerFactory
import java.util.UUID

class MsGraphTilgangsgruppehenter(
    private val accessTokenGenerator: AccessTokenGenerator,
    private val tilgangsgrupper: Tilgangsgrupper,
) : Tilgangsgruppehenter {
    private val logg = LoggerFactory.getLogger(MsGraphTilgangsgruppehenter::class.java)
    private val graphUrl = "https://graph.microsoft.com/v1.0"
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

    override suspend fun hentTilgangsgrupper(
        oid: UUID,
        gruppeIder: List<UUID>,
    ): Set<UUID> {
        val token = accessTokenGenerator.hentAccessToken("https://graph.microsoft.com/.default")
        val response =
            httpClient.post(graphUrl) {
                url {
                    path("v1.0/users/$oid/checkMemberGroups")
                }
                bearerAuth(token)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(mapOf("groupIds" to gruppeIder.map { it.toString() }))
            }

        val responseNode = objectMapper.readTree(response.bodyAsText())
        val grupper = responseNode["value"].map { UUID.fromString(it.asText()) }
        logg.debug("Hentet ${grupper.size} grupper fra MS")
        return grupper.toSet()
    }

    override suspend fun hentTilgangsgrupper(oid: UUID): Set<Tilgangsgruppe> {
        val gruppeUuider = hentTilgangsgrupper(oid, tilgangsgrupper.alleUuider().toList())
        return tilgangsgrupper.grupperFor(gruppeUuider)
    }
}
