package no.nav.helse.spesialist.api.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import no.nav.helse.spesialist.api.graphql.Modellfeil
import no.nav.helse.spesialist.application.logg.loggThrowable

fun StatusPagesConfig.configureStatusPagesPlugin() {
    exception<Modellfeil> { call: ApplicationCall, modellfeil: Modellfeil ->
        modellfeil.logger()
        call.respond(status = modellfeil.httpkode, message = modellfeil.tilFeilDto())
    }
    exception<Throwable> { call, cause ->
        val uri = call.request.uri
        val verb = call.request.httpMethod.value
        loggThrowable("Unhandled: $verb", uri, cause)
        call.respondText(
            text = "Det skjedde en uventet feil",
            status = HttpStatusCode.InternalServerError,
        )
        call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")
    }
}
