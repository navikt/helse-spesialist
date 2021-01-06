package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.mediator.OpptegnelseMediator
import no.nav.helse.mediator.api.modell.Saksbehandler

internal fun Route.opptegnelseApi(opptegnelseMediator: OpptegnelseMediator) {
    post("/api/opptegnelse/abonner/{aktørId}") {
        val aktørId = requireNotNull(call.parameters["aktørId"]?.toLong()) { "Ugyldig aktørId i path parameter" }
        val saksbehandler = Saksbehandler.fraToken(requireNotNull(call.principal()))

        opptegnelseMediator.opprettAbonnement(saksbehandler.oid, aktørId)
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    get("/api/opptegnelse/hent/{sisteSekvensId}") {
        val sisteSekvensId = requireNotNull(call.parameters["sisteSekvensId"]?.toInt()) { "Ugyldig aktørId i path parameter" }
        val saksbehandler = Saksbehandler.fraToken(requireNotNull(call.principal()))

        val oppdateringer = opptegnelseMediator.hentAbonnerteOpptegnelser(saksbehandler.oid, sisteSekvensId)
        call.respond(HttpStatusCode.OK, oppdateringer)
    }

    get("/api/opptegnelse/hent") {
        val saksbehandler = Saksbehandler.fraToken(requireNotNull(call.principal()))

        val oppdateringer = opptegnelseMediator.hentAbonnerteOpptegnelser(saksbehandler.oid)
        call.respond(HttpStatusCode.OK, oppdateringer)
    }
}
