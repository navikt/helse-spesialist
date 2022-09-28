package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.harTilgangTilBeslutteroppgaver
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.oppgave.OppgaveMediator
import org.slf4j.LoggerFactory

internal fun Route.personApi(hendelseMediator: HendelseMediator, oppgaveMediator: OppgaveMediator) {
    val log = LoggerFactory.getLogger("PersonApi")

    get("/api/person/aktorId/{aktørId}/sist_oppdatert") {
        call.parameters["aktørId"]?.toLongOrNull() ?: run {
            call.respond(status = HttpStatusCode.BadRequest, message = "AktørId må være numerisk")
            return@get
        }
    }

    get("/api/person/fnr/{fødselsnummer}/sist_oppdatert") {
        call.parameters["fødselsnummer"]?.toLongOrNull() ?: run {
            call.respond(status = HttpStatusCode.BadRequest, message = "Fødselsnummer må være numerisk")
            return@get
        }
    }

    post("/api/vedtak") {
        val godkjenning = call.receive<GodkjenningDTO>()
        log.info("Behandler godkjenning av ${godkjenning.oppgavereferanse}")
        val (oid, epostadresse) = requireNotNull(call.principal<JWTPrincipal>()).payload.let {
            UUID.fromString(it.getClaim("oid").asString()) to it.getClaim("preferred_username").asString()
        }

        val erAktivOppgave =
            withContext(Dispatchers.IO) { oppgaveMediator.erAktivOppgave(godkjenning.oppgavereferanse) }
        if (!erAktivOppgave) {
            call.respondText(
                "Dette vedtaket har ingen aktiv saksbehandleroppgave. Dette betyr vanligvis at oppgaven allerede er fullført.",
                status = HttpStatusCode.Conflict
            )
            return@post
        }

        val erBeslutteroppgave = oppgaveMediator.erBeslutteroppgave(godkjenning.oppgavereferanse)
        if(erBeslutteroppgave) {
            // Midlertidig logging. Slik at vi vet når vi kan skru av totrinnsmerking i Speil
            if(!oppgaveMediator.trengerTotrinnsvurdering(godkjenning.oppgavereferanse)) {
                log.info("Oppgave ${godkjenning.oppgavereferanse} er merket vha Speil.")
            }

            if(!harTilgangTilBeslutteroppgaver() && "dev-gcp" != System.getenv("NAIS_CLUSTER_NAME")) {
                call.respondText(
                    "Saksbehandler trenger beslutter-rolle for å kunne utbetale beslutteroppgaver",
                    status = HttpStatusCode.Unauthorized
                )
                return@post
            }
        }

        withContext(Dispatchers.IO) { hendelseMediator.håndter(godkjenning, epostadresse, oid) }
        call.respond(HttpStatusCode.Created, mapOf("status" to "OK"))
    }

    post("/api/person/oppdater") {
        val personoppdateringDto = call.receive<OppdaterPersonsnapshotDto>()
        hendelseMediator.håndter(personoppdateringDto)
        call.respond(HttpStatusCode.OK)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppdaterPersonsnapshotDto(
    val fødselsnummer: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GodkjenningDTO(
    val oppgavereferanse: Long,
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?
) {
    init {
        if (!godkjent) requireNotNull(årsak)
    }
}
