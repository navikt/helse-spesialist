package no.nav.helse.spesialist.api.utbetaling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDto.Companion.fraOnBehalfOfToken

fun Route.annulleringApi(saksbehandlerMediator: SaksbehandlerMediator) {
    post("/api/annullering") {
        withContext(Dispatchers.IO) {
            saksbehandlerMediator.håndter(call.receive<Annullering>(), fraOnBehalfOfToken(requireNotNull(call.principal())))
        }
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}