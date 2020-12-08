package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.mediator.AbonnementMediator
import no.nav.helse.mediator.api.modell.Saksbehandler

internal fun Route.abonnementApi(abonnementMediator: AbonnementMediator) {
    post("/api/abonner/{aktørId}") {
        val aktørId = requireNotNull(call.parameters["aktørId"]?.toLong()) { "Ugyldig aktørId i path parameter" }
        val saksbehandler = Saksbehandler.fraToken(requireNotNull(call.principal()))

        abonnementMediator.opprettAbonnement(saksbehandler.oid, aktørId)
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    get("/api/oppdatering/{sisteSekvensId}") {
        val sisteSekvensId = requireNotNull(call.parameters["sisteSekvensId"]?.toInt()) { "Ugyldig aktørId i path parameter" }
        val saksbehandler = Saksbehandler.fraToken(requireNotNull(call.principal()))

        val oppdateringer = abonnementMediator.hentOpptegnelser(saksbehandler.oid, sisteSekvensId)
        call.respond(HttpStatusCode.OK, oppdateringer)
    }
}
