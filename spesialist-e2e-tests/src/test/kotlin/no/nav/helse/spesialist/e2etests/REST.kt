package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.junit.jupiter.api.Assertions.assertTrue

object REST {
    private val httpClient: HttpClient =
        HttpClient(Apache5) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                socketTimeout = 0
                connectTimeout = 1_000
                connectionRequestTimeout = 1_000
            }
        }

    fun get(
        relativeUrl: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
    ): JsonNode {
        val url = "http://localhost:${E2ETestApplikasjon.port}/$relativeUrl"
        logg.info("Gjør HTTP GET $url")
        val (status, bodyAsText) =
            runBlocking {
                httpClient
                    .get(url) {
                        accept(ContentType.Application.Json)
                        bearerAuth(
                            E2ETestApplikasjon.apiModuleIntegrationTestFixture.token(
                                saksbehandler,
                                tilganger,
                                brukerroller,
                            ),
                        )
                    }.let { it.status to it.bodyAsText() }
            }
        logg.info("Respons fra HTTP GET: $bodyAsText")
        assertTrue(status.isSuccess()) { "Fikk HTTP-feilkode ${status.value} fra HTTP GET" }
        return objectMapper.readTree(bodyAsText)
    }

    fun patch(
        relativeUrl: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
        request: Any,
    ): JsonNode {
        val url = "http://localhost:${E2ETestApplikasjon.port}/$relativeUrl"
        logg.info("Gjør HTTP PATCH $url med body: ${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request)}")
        val (status, bodyAsText) =
            runBlocking {
                httpClient
                    .patch(url) {
                        bearerAuth(
                            E2ETestApplikasjon.apiModuleIntegrationTestFixture.token(
                                saksbehandler,
                                tilganger,
                                brukerroller,
                            ),
                        )
                        accept(ContentType.Application.Json)
                        contentType(ContentType.Application.Json)
                        if (request !is Unit) setBody(request)
                    }.let { it.status to it.bodyAsText() }
            }
        logg.info("Respons fra HTTP PATCH: $bodyAsText")
        assertTrue(status.isSuccess()) { "Fikk HTTP-feilkode ${status.value} fra HTTP PATCH" }
        return objectMapper.readTree(bodyAsText)
    }

    fun post(
        relativeUrl: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
        request: Any,
    ): JsonNode {
        val url = "http://localhost:${E2ETestApplikasjon.port}/$relativeUrl"
        logg.info("Gjør HTTP POST $url med body: ${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request)}")
        val (status, bodyAsText) =
            runBlocking {
                httpClient
                    .post(url) {
                        bearerAuth(
                            E2ETestApplikasjon.apiModuleIntegrationTestFixture.token(
                                saksbehandler,
                                tilganger,
                                brukerroller,
                            ),
                        )
                        accept(ContentType.Application.Json)
                        contentType(ContentType.Application.Json)
                        if (request !is Unit) setBody(request)
                    }.let { it.status to it.bodyAsText() }
            }
        logg.info("Respons fra HTTP POST: $bodyAsText")
        assertTrue(status.isSuccess()) { "Fikk HTTP-feilkode ${status.value} fra HTTP POST" }
        return objectMapper.readTree(bodyAsText)
    }

    fun put(
        relativeUrl: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
        request: Any,
    ): JsonNode {
        val url = "http://localhost:${E2ETestApplikasjon.port}/$relativeUrl"
        logg.info("Gjør HTTP PUT $url med body: ${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request)}")
        val (status, bodyAsText) =
            runBlocking {
                httpClient
                    .put(url) {
                        bearerAuth(
                            E2ETestApplikasjon.apiModuleIntegrationTestFixture.token(
                                saksbehandler,
                                tilganger,
                                brukerroller,
                            ),
                        )
                        accept(ContentType.Application.Json)
                        contentType(ContentType.Application.Json)
                        if (request !is Unit) setBody(request)
                    }.let { it.status to it.bodyAsText() }
            }
        logg.info("Respons fra HTTP PUT: $bodyAsText")
        assertTrue(status.isSuccess()) { "Fikk HTTP-feilkode ${status.value} fra HTTP PUT" }
        return objectMapper.readTree(bodyAsText)
    }
}
