package no.nav.helse.mediator.api

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.feilhåndtering.modellfeilForRest
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
            val ident = accessToken.payload.getClaim("NAVident").asString()

            withContext(Dispatchers.IO) {
                tildelingMediator.tildelOppgaveTilSaksbehandler(
                    oppgaveId,
                    saksbehandlerreferanse,
                    epostadresse,
                    navn,
                    ident
                )
            }
            log.info("Oppgave $oppgaveId er tildelt til $ident")
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
