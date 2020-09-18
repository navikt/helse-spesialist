package no.nav.helse.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.mediator.kafka.HendelseMediator

internal fun Application.adminApi(spleisbehovMediator: HendelseMediator) {
    routing {
        authenticate("admin") {
            route("/admin") {
                post("/rollback") {
                    val rollback = call.receive<Array<TilbakerullingDTO>>()
                    rollback.forEach {
                        spleisbehovMediator.håndter(it)
                    }
                    call.respond(HttpStatusCode.OK)
                }
                post("/rollback_delete") {
                    val rollback = call.receive<Array<TilbakerullingMedSlettingDTO>>()
                    rollback.forEach {
                        spleisbehovMediator.håndter(it)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

data class TilbakerullingDTO(val fødselsnummer: String, val aktørId: String, val personVersjon: Long)
data class TilbakerullingMedSlettingDTO(val fødselsnummer: String, val aktørId: String)
