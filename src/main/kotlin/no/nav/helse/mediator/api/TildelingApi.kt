package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.modell.feilhåndtering.modellfeilForRest
import no.nav.helse.modell.tildeling.TildelingMediator
import org.slf4j.LoggerFactory
import java.util.*

private val log = LoggerFactory.getLogger("TildelingApi")

internal fun Route.tildelingApi(tildelingMediator: TildelingMediator) {
    post("/api/tildeling/{oppgavereferanse}") {
        modellfeilForRest {
            val oppgaveId = call.parameters["oppgavereferanse"]?.toLongOrNull()
            if (oppgaveId == null) {
                call.respond(HttpStatusCode.BadRequest, "Ugyldig oppgavereferanse i path parameter")
                log.warn("POST - oppgavereferanse er null i path parameter")
                return@post
            }

            val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
            val saksbehandlerreferanse = UUID.fromString(accessToken.payload.getClaim("oid").asString())
            val epostadresse = accessToken.payload.getClaim("preferred_username").asString()
            val navn = accessToken.payload.getClaim("name").asString()

            withContext(Dispatchers.IO) {
                tildelingMediator.tildelOppgaveTilSaksbehandler(
                    oppgaveId,
                    saksbehandlerreferanse,
                    epostadresse,
                    navn
                )
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    delete("/api/tildeling/{oppgavereferanse}") {
        val oppgaveId = call.parameters["oppgavereferanse"]?.toLongOrNull()
        if (oppgaveId == null) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig oppgavereferanse i path parameter")
            log.warn("DELETE - oppgavereferanse er null i path parameter")
            return@delete
        }
        withContext(Dispatchers.IO) { tildelingMediator.fjernTildeling(oppgaveId) }

        call.respond(HttpStatusCode.OK)
    }
}
