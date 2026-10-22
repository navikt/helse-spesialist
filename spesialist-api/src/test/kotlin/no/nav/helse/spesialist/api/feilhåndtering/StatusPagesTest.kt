package no.nav.helse.spesialist.api.feilhåndtering

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableDeferred
import no.nav.helse.spesialist.api.plugins.configureStatusPagesPlugin
import tools.jackson.databind.DatabindException
import java.io.Closeable
import java.util.concurrent.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StatusPagesTest {
    class MyDatabindException(
        msg: String,
        cause: Throwable? = null,
    ) : DatabindException(null as Closeable?, msg, cause)

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
    fun `kansellert kall under serialisering gir ikke 500`() {
        testApplication {
            install(StatusPages) { configureStatusPagesPlugin() }
            routing {
                get("/test") {
                    val cause = CancellationException("Call context was cancelled by `HttpRequestLifecycle` plugin")
                    throw MyDatabindException(cause.message!!, cause)
                }
            }
            val response = client.get("/test")
            assertEquals(499, response.status.value)
        }
    }

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
