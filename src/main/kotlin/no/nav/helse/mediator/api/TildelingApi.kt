package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.modell.feilhåndtering.modellfeilForRest
import no.nav.helse.modell.tildeling.TildelingMediator
import java.util.*

internal fun Route.tildelingApi(tildelingMediator: TildelingMediator) {
    post("/api/v1/tildeling/{oppgavereferanse}") {
        modellfeilForRest {
            val oppgaveId =
                requireNotNull(call.parameters["oppgavereferanse"]?.toLong()) { "Ugyldig oppgavereferanse i path parameter" }
            val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
            val saksbehandlerreferanse = UUID.fromString(accessToken.payload.getClaim("oid").asString())
            val epostadresse = accessToken.payload.getClaim("preferred_username").asString()
            val navn = accessToken.payload.getClaim("name").asString()

            tildelingMediator.tildelOppgaveTilSaksbehandler(oppgaveId, saksbehandlerreferanse, epostadresse, navn)

            call.respond(HttpStatusCode.OK)
        }
    }

    delete("/api/v1/tildeling/{oppgavereferanse}") {
        val oppgaveId =
            requireNotNull(call.parameters["oppgavereferanse"]?.toLong()) { "Ugyldig oppgavereferanse i path parameter" }
        tildelingMediator.fjernTildeling(oppgaveId)

        call.respond(HttpStatusCode.OK)
    }
}
