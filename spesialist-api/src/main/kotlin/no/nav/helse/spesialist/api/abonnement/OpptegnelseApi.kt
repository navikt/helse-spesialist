package no.nav.helse.spesialist.api.abonnement

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.pipeline.*
import java.util.*

fun Route.opptegnelseApi(opptegnelseMediator: OpptegnelseMediator) {
    post("/api/opptegnelse/abonner/{aktørId}") {
        val saksbehandlerreferanse = getSaksbehandlerOid()
        val aktørId = call.parameters["aktørId"]!!.let {
            requireNotNull(it.toLongOrNull()) { "$it er ugyldig aktørId i path parameter" }
        }

        opptegnelseMediator.opprettAbonnement(saksbehandlerreferanse, aktørId)
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    get("/api/opptegnelse/hent/{sisteSekvensId}") {
        val saksbehandlerreferanse = getSaksbehandlerOid()

        val sisteSekvensId = call.parameters["sisteSekvensId"]!!.let {
            requireNotNull(it.toIntOrNull()) { "$it er ugyldig siste seksvensid i path parameter" }
        }
        val opptegnelser = opptegnelseMediator.hentAbonnerteOpptegnelser(saksbehandlerreferanse, sisteSekvensId)

        call.respond(HttpStatusCode.OK, opptegnelser)
    }

    get("/api/opptegnelse/hent") {
        val saksbehandlerreferanse = getSaksbehandlerOid()
        val opptegnelser = opptegnelseMediator.hentAbonnerteOpptegnelser(saksbehandlerreferanse)

        call.respond(HttpStatusCode.OK, opptegnelser)
    }
}

private fun PipelineContext<Unit, ApplicationCall>.getSaksbehandlerOid(): UUID {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return UUID.fromString(accessToken.payload.getClaim("oid").asString())
}
