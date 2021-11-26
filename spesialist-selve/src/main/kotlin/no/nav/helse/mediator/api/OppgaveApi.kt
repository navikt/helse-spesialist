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
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.oppgave.OppgavereferanseDto
import org.slf4j.LoggerFactory
import java.util.*

private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

internal fun Route.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    riskSupersaksbehandlergruppe: String
) {
    val gruppeIdForRiskSaksbehandlere = UUID.fromString(riskSupersaksbehandlergruppe)
    get("/api/oppgaver") {
        val saksbehandlerOppgaver = withContext(Dispatchers.IO) {
            val erRiskSupersaksbehandler = getGrupper().contains(gruppeIdForRiskSaksbehandlere)
            oppgaveMediator.hentOppgaver(erRiskSupersaksbehandler)
                .also { oppgaveliste ->
                    val riskoppgaver = oppgaveliste.filter { it.oppgavetype == "RISK_QA" }
                    val riskOppgaverLiggerKronologiske = riskoppgaver
                        .zipWithNext { a, b -> a.opprettet >= b.opprettet }.all { it }
                    if (!riskOppgaverLiggerKronologiske) sikkerLogg.info("Risk-oppgaver som ikke ligger kronologisk\n{}",
                    riskoppgaver.map { it.opprettet })
                }
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
                call.respond(OppgavereferanseDto(it))
            } ?: call.respond(HttpStatusCode.NotFound)
    }
}

private fun PipelineContext<Unit, ApplicationCall>.getGrupper(): List<UUID> {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return accessToken.payload.getClaim("groups").asList(String::class.java).map(UUID::fromString)
}
