package no.nav.helse

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
fun main(): Unit = runBlocking {
    val embeddedServer = embeddedServer(CIO, port = 8080) {
        routing {
            get("/isalive") {
                call.respondText("OK", status = HttpStatusCode.OK)
            }
            get("/isready") {
                call.respondText("OK", status = HttpStatusCode.OK)
            }
        }
    }.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        embeddedServer.stop(5, 5, TimeUnit.SECONDS)
    })
}