package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableDeferred
import no.nav.helse.spesialist.api.plugins.configureContentNegotiationPlugin
import no.nav.helse.spesialist.api.plugins.configureStatusPagesPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class StatusPagesTest {
    // Uten svar fra handleren logger Ktor-fallbacken ERROR og svarer 500
    @Test
    fun `kansellering ved nedstenging gir 503`() {
        testApplication {
            install(StatusPages) { configureStatusPagesPlugin() }
            routing {
                get("/test") {
                    // await på kansellert Deferred kaster en ekte JobCancellationException, som ved nedstenging
                    CompletableDeferred<Unit>().apply { cancel() }.await()
                }
            }
            val response = client.get("/test")
            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        }
    }

    @Test
    fun `JSON-svar serialiseres ferdig før sending`() {
        testApplication {
            install(ContentNegotiation) { configureContentNegotiationPlugin() }
            install(StatusPages) { configureStatusPagesPlugin() }
            routing {
                get("/test") { call.respond(Svar(verdi = 1)) }
            }
            val response = client.get("/test")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("""{"verdi":1}""", response.bodyAsText())
            assertEquals("11", response.headers[HttpHeaders.ContentLength])
            assertNull(response.headers[HttpHeaders.TransferEncoding])
        }
    }

    @Test
    fun `feil under serialisering gir 500, ikke 200 med avkuttet kropp`() {
        testApplication {
            install(ContentNegotiation) { configureContentNegotiationPlugin() }
            install(StatusPages) { configureStatusPagesPlugin() }
            routing {
                get("/test") { call.respond(SvarSomFeiler()) }
            }
            val response = client.get("/test")
            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }
}

private data class Svar(
    val verdi: Int,
)

private class SvarSomFeiler {
    @Suppress("unused")
    val verdi: Int get() = error("Feil under serialisering")
}
