package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.mediator.HendelseMediator

internal fun Application.adminApi(mediator: HendelseMediator) {
    routing {
        authenticate("admin") {
            route("/admin") {
                post("/rollback") {
                    val rollback = call.receive<Array<TilbakerullingDTO>>()
                    rollback.forEach {
                        mediator.håndter(it)
                    }
                    call.respond(HttpStatusCode.OK)
                }
                post("/rollback_delete") {
                    val rollback = call.receive<Array<TilbakerullingMedSlettingDTO>>()
                    rollback.forEach {
                        mediator.håndter(it)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

data class TilbakerullingDTO(val fødselsnummer: String, val aktørId: String, val personVersjon: Long)
data class TilbakerullingMedSlettingDTO(val fødselsnummer: String, val aktørId: String)
