package no.nav.helse.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.*

internal fun Route.oppgaveApi(
    oppgaveMediator: OppgaveMediator
) {
    get("/api/oppgaver") {
        val saksbehandlerOppgaver = oppgaveMediator.hentOppgaver()
        if (saksbehandlerOppgaver.isEmpty()) {
            call.respond(HttpStatusCode.NotFound, "Fant ingen oppgaver")
            return@get
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

        val oppgavereferanse = oppgaveMediator.hentOppgave(fødselsnummer)?.eventId
        if (oppgavereferanse == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(no.nav.helse.modell.vedtak.SaksbehandleroppgavereferanseDto(oppgavereferanse))
        }
    }
}
