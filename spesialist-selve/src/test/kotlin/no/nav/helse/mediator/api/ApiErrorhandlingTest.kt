package no.nav.helse.mediator.api

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.testing.*
import no.nav.helse.bootstrap.installErrorHandling
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApiErrorhandlingTest {
    @Test
    fun `vi logger ikke ut fnr i url om en request bobler helt opp`() {
        testApplication {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            this.application { installErrorHandling() }

            routing {
                get("/dummy/{fnr}") {
                    throw IllegalArgumentException("fail")
                }
            }

            val response = client.get("/dummy/123")

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals("Det skjedde en uventet feil", response.bodyAsText())
        }
    }
}
