package no.nav.helse.mediator.api

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.feilhåndtering.modellfeilForRest
import no.nav.helse.modell.leggpåvent.LeggPåVentMediator
import no.nav.helse.notat.NotatMediator
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("LeggPåVentApi")

internal fun Route.leggPåVentApi(leggPåVentMediator: LeggPåVentMediator, notatMediator: NotatMediator) {
    post("/api/leggpaavent/{oppgavereferanse}") {
        modellfeilForRest {
            val oppgaveId = call.parameters["oppgavereferanse"]?.toLongOrNull()

            if (oppgaveId == null) {
                call.respond(HttpStatusCode.BadRequest, "Ugyldig oppgavereferanse i path parameter")
                log.warn("POST - oppgavereferanse er null i path parameter")
                return@post
            }

            val notat = call.receive<NotatApiDto>()
            val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
            val saksbehandlerOid = UUID.fromString(accessToken.payload.getClaim("oid").asString())
            notatMediator.lagreForOppgaveId(oppgaveId, notat.tekst, saksbehandlerOid, notat.type)

            withContext(Dispatchers.IO) {
                leggPåVentMediator.leggOppgavePåVent(oppgaveId)
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    delete("/api/leggpaavent/{oppgavereferanse}") {
        val oppgaveId = call.parameters["oppgavereferanse"]?.toLongOrNull()
        if (oppgaveId == null) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig oppgavereferanse i path parameter")
            log.warn("DELETE - oppgavereferanse er null i path parameter")
            return@delete
        }
        withContext(Dispatchers.IO) { leggPåVentMediator.fjernPåVent(oppgaveId) }

        call.respond(HttpStatusCode.OK)
    }
}
