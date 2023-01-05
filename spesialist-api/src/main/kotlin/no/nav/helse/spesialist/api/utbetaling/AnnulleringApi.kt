package no.nav.helse.spesialist.api.utbetaling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDto

fun Route.annulleringApi(saksbehandlerMediator: SaksbehandlerMediator) {
    post("/api/annullering") {
        saksbehandlerMediator.håndter(call.receive<Annullering>(), SaksbehandlerDto.fraOnBehalfOfToken(requireNotNull(call.principal())))
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}