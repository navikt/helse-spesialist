package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.modell.Saksbehandler

internal fun Route.annulleringApi(hendelseMediator: HendelseMediator) {
    post("/api/annullering") {
        val annullering = call.receive<AnnulleringDto>()
        val saksbehandler = Saksbehandler.fraToken(requireNotNull(call.principal()))

        hendelseMediator.håndter(annullering, saksbehandler)
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}

@JsonIgnoreProperties
data class AnnulleringDto(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val fagsystemId: String
)


