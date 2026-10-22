package no.nav.helse.spesialist.api.sse

import io.ktor.server.application.findPluginInRoute
import io.ktor.server.application.plugin
import io.ktor.server.http.HttpRequestLifecycle
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class SseRouteTest {
    @Test
    fun `HttpRequestLifecycle gjelder kun SSE-ruta og ikke naboruter`() {
        testApplication {
            install(SSE)
            application {
                routing {
                    sse(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
                    get("/annen") { call.respondText("ok") }
                }

                val noder = plugin(RoutingRoot).alleNoder()
                val sseNode = noder.last { it.toString().contains("/personer/{personPseudoId}/sse") }
                val annenNode = noder.last { it.toString().contains("/annen") }

                assertNotNull(sseNode.findPluginInRoute(HttpRequestLifecycle))
                assertNull(annenNode.findPluginInRoute(HttpRequestLifecycle))
            }
            startApplication()
        }
    }
}

private fun RoutingNode.alleNoder(): List<RoutingNode> = listOf(this) + children.flatMap { it.alleNoder() }
