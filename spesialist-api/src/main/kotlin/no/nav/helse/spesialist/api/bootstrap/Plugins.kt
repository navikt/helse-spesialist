package no.nav.helse.spesialist.api.bootstrap

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.websocket.WebSockets
import no.nav.helse.spesialist.api.feilhåndtering.Modellfeil
import org.slf4j.LoggerFactory

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
private val logg = LoggerFactory.getLogger("SpesialistApp")

internal fun Application.installPlugins() {
    install(WebSockets)
    install(DoubleReceive)
    requestResponseTracing(sikkerlogg)
}

fun StatusPagesConfig.configureStatusPages() {
    exception<Modellfeil> { call: ApplicationCall, modellfeil: Modellfeil ->
        modellfeil.logger()
        call.respond(status = modellfeil.httpkode, message = modellfeil.tilFeilDto())
    }
    exception<Throwable> { call, cause ->
        val uri = call.request.uri
        val verb = call.request.httpMethod.value
        logg.error("Unhandled: $verb", cause)
        sikkerlogg.error("Unhandled: $verb - $uri", cause)
        call.respondText(
            text = "Det skjedde en uventet feil",
            status = HttpStatusCode.InternalServerError,
        )
        call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")
    }
}
