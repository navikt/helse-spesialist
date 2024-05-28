package no.nav.helse.spesialist.api.websockets

import io.ktor.server.application.ApplicationStopped
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.nio.charset.Charset

fun Route.webSocketsApi() {
    val log = LoggerFactory.getLogger("WebSocketsApi")

    val sessions = mutableSetOf<WebSocketSession>()

    environment?.monitor?.subscribe(ApplicationStopped) {
        val sessionsToClose = sessions.filter { it.isActive }
        log.info("Closing ${sessionsToClose.size} WebSocket sessions")
        runBlocking { sessionsToClose.forEach { it.close() } }
    }

    fun Route.testRoute() {
        webSocket("/opptegnelse") {
            log.info("WebSocket åpnet, session: $this")
            sessions.add(this)

            outgoing.send(Frame.Text("Du er nå koblet til meg (Spesialist)"))

            for (frame in incoming) {
                log.info("Incoming frame: ${frame.data.toString(Charset.defaultCharset())}")
                outgoing.send(Frame.Text("Melding via WebSocket fra Spesialist"))
            }
        }
    }

    route("/ws") {
        authenticate("oidc") {
            route("/sikret") {
                testRoute()
            }
        }
        testRoute()
    }
}
