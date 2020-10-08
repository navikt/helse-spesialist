package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.mediator.HendelseMediator
import java.time.LocalDate
import java.util.*

internal fun Route.annulleringApi(hendelseMediator: HendelseMediator) {
    post("/api/v1/annullering") {
        val annullering = call.receive<AnnulleringDto>()
        val oid = requireNotNull(call.principal<JWTPrincipal>())
            .payload.getClaim("oid").asString().let { UUID.fromString(it) }
        val epostadresse = requireNotNull(call.principal<JWTPrincipal>())
            .payload.getClaim("preferred_username").asString()

        hendelseMediator.håndter(annulleringDto = annullering, saksbehandlerOid = oid, epostadresse = epostadresse)
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}

@JsonIgnoreProperties
data class AnnulleringDto(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val dager: List<LocalDate>
)
