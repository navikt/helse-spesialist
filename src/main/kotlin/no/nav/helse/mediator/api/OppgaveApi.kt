package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.vedtak.SaksbehandleroppgavereferanseDto
import java.util.*

internal fun Route.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    riskSupersaksbehandlergruppe: String
) {
    val riskSupersaksbehandlergruppe = UUID.fromString(riskSupersaksbehandlergruppe)
    get("/api/oppgaver") {
        val saksbehandlerOppgaver = withContext(Dispatchers.IO) {
            oppgaveMediator.hentOppgaver(getGrupper().contains(riskSupersaksbehandlergruppe))
        }
        call.respond(saksbehandlerOppgaver)
    }
}

internal fun Route.direkteOppgaveApi(oppgaveMediator: OppgaveMediator) {
    get("/api/v1/oppgave") {
        val fødselsnummer = call.request.header("fodselsnummer")
        if (fødselsnummer == null) {
            call.respond(HttpStatusCode.BadRequest, "Mangler fødselsnummer i header")
            return@get
        }

        withContext(Dispatchers.IO) { oppgaveMediator.hentOppgaveId(fødselsnummer) }
            ?.let {
                call.respond(SaksbehandleroppgavereferanseDto(it))
            } ?: call.respond(HttpStatusCode.NotFound)
    }
}

private fun PipelineContext<Unit, ApplicationCall>.getGrupper(): List<UUID> {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return accessToken.payload.getClaim("groups").asList(String::class.java).map(UUID::fromString)
}
