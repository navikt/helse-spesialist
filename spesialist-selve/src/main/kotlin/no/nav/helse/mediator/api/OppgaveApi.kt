package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.getGrupper
import no.nav.helse.oppgave.OppgaveMediator
import java.util.*

internal fun Route.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    riskSupersaksbehandlergruppe: String
) {
    val gruppeIdForRiskSaksbehandlere = UUID.fromString(riskSupersaksbehandlergruppe)
    get("/api/oppgaver") {
        val saksbehandlerOppgaver = withContext(Dispatchers.IO) {
            val erRiskSupersaksbehandler = getGrupper().contains(gruppeIdForRiskSaksbehandlere)
            oppgaveMediator.hentOppgaver(erRiskSupersaksbehandler)
        }
        call.respond(saksbehandlerOppgaver)
    }
}

