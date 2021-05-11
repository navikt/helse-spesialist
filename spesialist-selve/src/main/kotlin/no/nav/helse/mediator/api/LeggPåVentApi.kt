package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.feilhåndtering.modellfeilForRest
import no.nav.helse.modell.leggpåvent.LeggPåVentMediator
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("LeggPåVentApi")

internal fun Route.leggPåVentApi(leggPåVentMediator: LeggPåVentMediator) {
    post("/api/leggpåvent/{oppgavereferanse}") {
        modellfeilForRest {
            val oppgaveId = call.parameters["oppgavereferanse"]?.toLongOrNull()
            if (oppgaveId == null) {
                call.respond(HttpStatusCode.BadRequest, "Ugyldig oppgavereferanse i path parameter")
                log.warn("POST - oppgavereferanse er null i path parameter")
                return@post
            }

            withContext(Dispatchers.IO) {
                leggPåVentMediator.leggOppgavePåVent(oppgaveId)
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    delete("/api/leggpåvent/{oppgavereferanse}") {
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
