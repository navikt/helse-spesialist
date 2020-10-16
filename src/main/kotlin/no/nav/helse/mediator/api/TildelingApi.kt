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

private val secureLog = LoggerFactory.getLogger("tjenestekall")

internal fun Route.tildelingApi(tildelingMediator: TildelingMediator) {
    post("/api/v1/tildeling/{oppgavereferanse}") {
        val ref = UUID.randomUUID()
        try {
            modellfeilForRest {
                val oppgaveId =
                    requireNotNull(call.parameters["oppgavereferanse"]?.toLong()) { "Ugyldig oppgavereferanse i path parameter" }
                secureLog.info("Tildeler oppgave med oppgaveid $oppgaveId (ref: $ref)")
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
        } catch (e: Throwable) {
            secureLog.warn("Feil i tildeling (ref: $ref)", e)
            call.respond(HttpStatusCode.InternalServerError)
            throw e
        } finally {
            secureLog.info("Ferdig med tildeling (ref: $ref)")
        }
    }

    delete("/api/v1/tildeling/{oppgavereferanse}") {
        val oppgaveId =
            requireNotNull(call.parameters["oppgavereferanse"]?.toLong()) { "Ugyldig oppgavereferanse i path parameter" }
        secureLog.info("Sletter tildeling for oppgave med oppgaveid $oppgaveId")
        withContext(Dispatchers.IO) { tildelingMediator.fjernTildeling(oppgaveId) }

        call.respond(HttpStatusCode.OK)
    }

    post("/api/v1/dummytildeling/{oppgavereferanse}") {
        modellfeilForRest {
            val oppgaveId =
                requireNotNull(call.parameters["oppgavereferanse"]?.toLong()) { "Ugyldig oppgavereferanse i path parameter" }
            secureLog.info("Dummy-tildeler oppgave med oppgaveid $oppgaveId")
            val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
            val saksbehandlerreferanse = UUID.fromString(accessToken.payload.getClaim("oid").asString())
            val epostadresse = accessToken.payload.getClaim("preferred_username").asString()
            val navn = accessToken.payload.getClaim("name").asString()

            call.respond(HttpStatusCode.OK)
        }
    }

    delete("/api/v1/dummytildeling/{oppgavereferanse}") {
        val oppgaveId =
            requireNotNull(call.parameters["oppgavereferanse"]?.toLong()) { "Ugyldig oppgavereferanse i path parameter" }
        secureLog.info("Dummy-sletter tildeling for oppgave med oppgaveid $oppgaveId")
        call.respond(HttpStatusCode.OK)
    }

}
