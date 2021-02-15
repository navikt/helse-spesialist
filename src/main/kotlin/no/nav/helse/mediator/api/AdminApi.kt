package no.nav.helse.mediator.api

import com.fasterxml.jackson.databind.node.ArrayNode
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
                post("/send") {
                    val request = call.receive<ArrayNode>()
                    request.forEach { node ->
                        mediator.sendMeldingPåTopic(node)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
