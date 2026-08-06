package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import no.nav.helse.spesialist.api.plugins.configureStatusPagesPlugin
import tools.jackson.databind.DatabindException
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StatusPagesTest {
    class MyDatabindException(
        msg: String,
    ) : DatabindException(msg)

    @Test
    fun `broken pipe i DatabindException gir ikke 500`() {
        testApplication {
            install(StatusPages) { configureStatusPagesPlugin() }
            routing {
                get("/test") {
                    throw MyDatabindException("Broken pipe")
                }
            }
            val response = client.get("/test")
            assertEquals(499, response.status.value)
        }
    }

    @Test
    fun `Connection reset by peer i DatabindException gir ikke 500`() {
        testApplication {
            install(StatusPages) { configureStatusPagesPlugin() }
            routing {
                get("/test") {
                    throw MyDatabindException("Connection reset by peer")
                }
            }
            val response = client.get("/test")
            assertEquals(499, response.status.value)
        }
    }

    @Test
    fun `annen DatabindException gir 500`() {
        testApplication {
            install(StatusPages) { configureStatusPagesPlugin() }
            routing {
                get("/test") {
                    throw MyDatabindException("noko gale")
                }
            }

            val response = client.get("/test")
            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }
}
