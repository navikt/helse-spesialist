package no.nav.helse.spesialist.api.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.utils.io.ClosedWriteChannelException
import no.nav.helse.spesialist.api.graphql.Modellfeil
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo

fun StatusPagesConfig.configureStatusPagesPlugin() {
    exception<Modellfeil> { call: ApplicationCall, modellfeil: Modellfeil ->
        modellfeil.logger()
        call.respond(status = modellfeil.httpkode, message = modellfeil.tilFeilDto())
    }
    exception<ClosedWriteChannelException> { call, exception ->
        val uri = call.request.uri
        if (!uri.endsWith("/sse")) return@exception onThrowable(call, exception)
        loggInfo("SSE-tilkobling lukket")
    }
    exception<Throwable> { call, cause ->
        onThrowable(call, cause)
    }
}

private suspend fun StatusPagesConfig.onThrowable(
    call: ApplicationCall,
    cause: Throwable,
) {
    val uri = call.request.uri
    val verb = call.request.httpMethod.value
    loggError("Unhandled: $verb $uri", cause)
    call.respondText(
        text = "Det skjedde en uventet feil",
        status = HttpStatusCode.InternalServerError,
    )
    call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")
}
