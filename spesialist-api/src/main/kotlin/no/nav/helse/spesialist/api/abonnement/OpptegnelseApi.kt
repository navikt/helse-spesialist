package no.nav.helse.spesialist.api.abonnement

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import org.slf4j.LoggerFactory

fun Route.opptegnelseApi() {
    val log = LoggerFactory.getLogger("OpptegnelseApi")

    webSocket("/ws/opptegnelse") {
        log.info("WebSocket opptegnelse")
        for (frame in incoming) {
            log.info("Incoming frame $frame")
            this.outgoing.send(Frame.Text("Melding via WebSocket fra Spesialist"))
        }
    }
}