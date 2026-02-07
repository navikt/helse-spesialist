package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.URI

object REST {
    fun get(
        relativeUrl: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
    ): JsonNode? =
        utførHttpKall(
            method = Method.GET,
            relativeUrl = relativeUrl,
            request = null,
            saksbehandler = saksbehandler,
            tilganger = tilganger,
            brukerroller = brukerroller
        )

    fun patch(
        relativeUrl: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
        request: Any,
    ): JsonNode? =
        utførHttpKall(
            method = Method.PATCH,
            relativeUrl = relativeUrl,
            request = request,
            saksbehandler = saksbehandler,
            tilganger = tilganger,
            brukerroller = brukerroller
        )

    fun post(
        relativeUrl: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
        request: Any,
    ): JsonNode? =
        utførHttpKall(
            method = Method.POST,
            relativeUrl = relativeUrl,
            request = request,
            saksbehandler = saksbehandler,
            tilganger = tilganger,
            brukerroller = brukerroller
        )

    fun put(
        relativeUrl: String,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>,
        request: Any,
    ): JsonNode? =
        utførHttpKall(
            method = Method.PUT,
            relativeUrl = relativeUrl,
            request = request,
            saksbehandler = saksbehandler,
            tilganger = tilganger,
            brukerroller = brukerroller
        )

    private fun utførHttpKall(
        method: Method,
        relativeUrl: String,
        request: Any?,
        saksbehandler: Saksbehandler,
        tilganger: Set<Tilgang>,
        brukerroller: Set<Brukerrolle>
    ): JsonNode? {
        val url = "http://localhost:${E2ETestApplikasjon.port}/$relativeUrl"
        val requestBodyAsString = request?.let(objectMapper.writerWithDefaultPrettyPrinter()::writeValueAsString)
        logg.info("Gjør HTTP $method $url" + requestBodyAsString?.let { " med body:\n$it" }.orEmpty())
        val (statusCode, responseBodyAsString) =
            Request
                .create(method, URI.create(url))
                .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
                .setHeader(
                    "Authorization", "Bearer ${
                        E2ETestApplikasjon.apiModuleIntegrationTestFixture.token(
                            saksbehandler = saksbehandler,
                            tilganger = tilganger,
                            brukerroller = brukerroller,
                        )
                    }"
                )
                .connectTimeout(Timeout.ofSeconds(10))
                .responseTimeout(Timeout.ofSeconds(10))
                .apply { requestBodyAsString?.let { bodyString(it, ContentType.APPLICATION_JSON) } }
                .execute()
                .handleResponse { response -> response.code to response.entity?.let(EntityUtils::toString) }
        logg.info(buildString {
            append("Respons fra HTTP $method: HTTP $statusCode")
            if (responseBodyAsString  != null) {
                append(" med body:\n$responseBodyAsString")
            } else {
                append(" uten body")
            }
        })
        assertTrue(statusCode in 200..299) { "Fikk HTTP-kode $statusCode fra HTTP $method" }
        return responseBodyAsString?.let(objectMapper::readTree)
    }
}
