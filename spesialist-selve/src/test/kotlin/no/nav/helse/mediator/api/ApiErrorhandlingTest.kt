package no.nav.helse.mediator.api

import io.ktor.http.*
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import no.nav.helse.installErrorHandling
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApiErrorhandlingTest {

    @Test
    fun `vi logger ikke ut fnr i url om en request bobler helt opp`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            installErrorHandling()
            routing {
                get("/dummy/{fnr}") {
                    throw IllegalArgumentException("fail")
                }
            }
        }) {
            with(handleRequest(HttpMethod.Get, "/dummy/123")) {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
                assertEquals("Det skjedde en uventet feil", response.content)
            }
        }
    }
}
