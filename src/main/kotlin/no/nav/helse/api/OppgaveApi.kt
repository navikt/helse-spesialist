package no.nav.helse.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import no.nav.helse.modell.vedtak.SaksbehandleroppgavereferanseDto

internal fun Application.oppgaveApi(
    oppgaveMediator: OppgaveMediator
) {
    routing {
        authenticate("saksbehandler") {
            get("/api/oppgaver") {
                val saksbehandlerOppgaver = oppgaveMediator.hentOppgaver()
                if (saksbehandlerOppgaver.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound, "Fant ingen oppgaver")
                    return@get
                }
                call.respond(saksbehandlerOppgaver)
            }

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
                    call.respond(SaksbehandleroppgavereferanseDto(oppgavereferanse))
                }
            }
        }
    }
}
