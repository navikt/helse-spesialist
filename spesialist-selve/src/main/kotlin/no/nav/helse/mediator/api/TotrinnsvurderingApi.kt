package no.nav.helse.mediator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID
import no.nav.helse.oppgave.OppgaveMediator
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TotrinnsvurderingApi")

internal fun Route.totrinnsvurderingApi(oppgaveMediator: OppgaveMediator) {
    post("/api/totrinnsvurdering/{oppgavereferanse}") {
        val oppgaveId = call.parameters["oppgavereferanse"]?.toLongOrNull()
        if (oppgaveId == null) {
            call.respond(HttpStatusCode.BadRequest, "Ugyldig oppgavereferanse i path parameter")
            log.warn("POST - oppgavereferanse er null i path parameter")
            return@post
        }

        val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
        val saksbehandlerOid = UUID.fromString(accessToken.payload.getClaim("oid").asString())

        oppgaveMediator.setBeslutterOppgave(
            oppgaveId = oppgaveId,
            erBeslutterOppgave = true,
            erReturOppgave = false,
            tidligereSaksbehandlerOID = saksbehandlerOid
        )
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}
