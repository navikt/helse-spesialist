package no.nav.helse.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing

internal fun Application.oppgaveApi(
    oppgaveMediator: OppgaveMediator
) {
    routing {
        authenticate {
            get("/api/oppgaver") {
                val saksbehandlerOppgaver = oppgaveMediator.hentOppgaver()
                if (saksbehandlerOppgaver.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound, "Fant ingen oppgaver")
                    return@get
                }
                call.respond(saksbehandlerOppgaver)
            }
        }
    }
}
