package no.nav.helse.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import no.nav.helse.mediator.kafka.SpleisbehovMediator

internal fun Application.adminApi(spleisbehovMediator: SpleisbehovMediator) {
    routing {
        authenticate("admin") {
            route("/admin") {
                post("/rollback") {
                    val rollback = call.receive<Array<Rollback>>()
                    rollback.forEach {
                        spleisbehovMediator.rollbackPerson(it)
                    }
                    call.respond(HttpStatusCode.OK)
                }
                post("/rollback_delete") {
                    val rollback = call.receive<Array<RollbackDelete>>()
                    rollback.forEach {
                        spleisbehovMediator.rollbackDeletePerson(it)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

data class Rollback(val fødselsnummer: String, val aktørId: String, val personVersjon: Long)
data class RollbackDelete(val fødselsnummer: String, val aktørId: String)
