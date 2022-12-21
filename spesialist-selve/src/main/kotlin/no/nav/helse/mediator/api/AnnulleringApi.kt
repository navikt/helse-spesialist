package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler

internal fun Route.annulleringApi(hendelseMediator: HendelseMediator) {
    post("/api/annullering") {
        val annullering = call.receive<AnnulleringDto>()
        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        hendelseMediator.håndter(annullering, saksbehandler)
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}

@JsonIgnoreProperties
data class AnnulleringDto(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val saksbehandlerIdent: String,
    val begrunnelser: List<String> = emptyList(),
    val gjelderSisteSkjæringstidspunkt: Boolean,
    val kommentar: String?
)


