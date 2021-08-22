package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.notat.NotatDto
import no.nav.helse.notat.NotatMediator
import org.slf4j.LoggerFactory
import java.util.*

private val log = LoggerFactory.getLogger("NotatApi")

internal fun Route.notaterApi(mediator: NotatMediator) {

    post("/api/notater/{oppgave_ref}") {
        val notat = call.receive<NotatDto>()

        val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
        val saksbehandler_oid = UUID.fromString(accessToken.payload.getClaim("oid").asString())

        val oppgave_ref = call.parameters["oppgave_ref"]
        if (oppgave_ref == null) {
            call.respond(HttpStatusCode.BadRequest, "oppgave_ref er ikke oppgitt")
            log.warn("POST - oppgavereferanse er null i path parameter")
            return@post
        }
        withContext(Dispatchers.IO) {
            mediator.lagre(oppgave_ref.toInt(), notat, saksbehandler_oid)
        }
    }

    get("/api/notater/{oppgave_ref}") {
        val oppgave_ref = call.parameters["oppgave_ref"]
        if (oppgave_ref == null) {
            call.respond(HttpStatusCode.BadRequest, "oppgave_ref er ikke oppgitt")
            log.warn("GET - oppgavereferanse er null i path parameter")
            return@get
        }

        val notater = withContext(Dispatchers.IO) { mediator.finn(oppgave_ref.toInt()) }
        call.respond(notater)
    }

}
