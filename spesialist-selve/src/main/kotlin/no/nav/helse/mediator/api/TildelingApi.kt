package no.nav.helse.mediator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.modell.tildeling.TildelingService
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TildelingApi")

internal fun Route.tildelingApi(
    tildelingService: TildelingService,
    tilgangsgrupper: Tilgangsgrupper,
) {
    post("/api/tildeling/{oppgavereferanse}") {
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
            tildelingService.tildelOppgaveTilSaksbehandler(
                oppgaveId,
                saksbehandlerreferanse,
                epostadresse,
                navn,
                ident,
                tilganger(tilgangsgrupper),
            )
        }
        log.info("Oppgave $oppgaveId er tildelt til $ident")
        call.respond(HttpStatusCode.OK)
    }

    delete("/api/tildeling/{oppgavereferanse}") {
        val oppgaveId = call.parameters["oppgavereferanse"]?.toLongOrNull()
        if (oppgaveId == null) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig oppgavereferanse i path parameter")
            log.warn("DELETE - oppgavereferanse er null i path parameter")
            return@delete
        }
        withContext(Dispatchers.IO) { tildelingService.fjernTildeling(oppgaveId) }

        call.respond(HttpStatusCode.OK)
    }
}
