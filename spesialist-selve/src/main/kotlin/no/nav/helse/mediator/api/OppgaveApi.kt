package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.SaksbehandlerTilganger
import no.nav.helse.getGrupper
import no.nav.helse.oppgave.OppgaveMediator
import java.util.*

internal fun Route.oppgaveApi(
    oppgaveMediator: OppgaveMediator,
    riskSupersaksbehandlergruppe: String,
    kode7Saksbehandlergruppe: String
) {
    val gruppeIdForKode7Saksbehandler = UUID.fromString(kode7Saksbehandlergruppe)
    val gruppeIdForRiskSaksbehandlere = UUID.fromString(riskSupersaksbehandlergruppe)
    get("/api/oppgaver") {
        val saksbehandlerOppgaver = withContext(Dispatchers.IO) {
            oppgaveMediator.hentOppgaver(saksbehandlerTilganger = SaksbehandlerTilganger(
                gruppetilganger = getGrupper(),
                kode7Saksbehandlergruppe = gruppeIdForKode7Saksbehandler,
                riskSaksbehandlergruppe = gruppeIdForRiskSaksbehandlere
            ))
        }
        call.respond(saksbehandlerOppgaver)
    }
}

