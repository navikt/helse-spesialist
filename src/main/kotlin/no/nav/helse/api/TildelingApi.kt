package no.nav.helse.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.tildeling.TildelingMediator
import java.util.*

internal fun Route.tildelingApi(tildelingMediator: TildelingMediator) {
    post("/api/v1/tildeling/{oppgavereferanse}/selv") {
        val oppgavereferanse =
            UUID.fromString(requireNotNull(call.parameters["oppgavereferanse"]) { "Ugyldig oppgavereferanse i path parameter" })
        val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
        val saksbehandlerreferanse = UUID.fromString(accessToken.payload.getClaim("oid").asString())
        tildelingMediator.tildelOppgaveTilSaksbehandler(oppgavereferanse, saksbehandlerreferanse)

        call.respond(HttpStatusCode.OK)
    }

    post("/api/v1/tildeling/{oppgavereferanse}/fjern") {
        val oppgavereferanse =
            UUID.fromString(requireNotNull(call.parameters["oppgavereferanse"]) { "Ugyldig oppgavereferanse i path parameter" })
        tildelingMediator.fjernTildeling(oppgavereferanse)

        call.respond(HttpStatusCode.OK)
    }
}
