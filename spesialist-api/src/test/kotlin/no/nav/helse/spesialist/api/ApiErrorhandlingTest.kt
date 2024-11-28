package no.nav.helse.spesialist.api

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import no.nav.helse.spesialist.api.bootstrap.configureStatusPages
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApiErrorhandlingTest {
    @Test
    fun `vi logger ikke ut fnr i url om en request bobler helt opp`() {
        testApplication {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            install(StatusPages) {
                configureStatusPages()
            }

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
