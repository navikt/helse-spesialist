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
import java.lang.Long.parseLong
import java.util.*

private val secureLog = LoggerFactory.getLogger("tjenestekall")

internal fun Route.tildelingApi(tildelingMediator: TildelingMediator) {
    post("/api/tildeling/{oppgavereferanse}") {
        val ref = UUID.randomUUID()
        try {
            modellfeilForRest {
                val oppgaveId = try {
                    parseLong(call.parameters["oppgavereferanse"])
                } catch (e: NumberFormatException) {
                    call.respond(HttpStatusCode.BadRequest, "Requesten må inneholde en gyldig oppgaveId")
                    return@post
                }

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

    delete("/api/tildeling/{oppgavereferanse}") {
        val oppgaveId = call.parameters["oppgavereferanse"]!!.let {
            requireNotNull(it.toLongOrNull()) { "$it er ugyldig oppgavereferanse i path parameter" }
        }
        secureLog.info("Sletter tildeling for oppgave med oppgaveid $oppgaveId")
        withContext(Dispatchers.IO) { tildelingMediator.fjernTildeling(oppgaveId) }

        call.respond(HttpStatusCode.OK)
    }

    post("/api/dummytildeling/{oppgavereferanse}") {
        modellfeilForRest {
            val oppgaveId =call.parameters["oppgavereferanse"]!!.let {
                requireNotNull(it.toLongOrNull()) { "$it er ugyldig oppgavereferanse i path parameter" }
            }
            secureLog.info("Dummy-post-tildeler oppgave med oppgaveid $oppgaveId")

            call.respond(HttpStatusCode.OK)
        }
    }
}
