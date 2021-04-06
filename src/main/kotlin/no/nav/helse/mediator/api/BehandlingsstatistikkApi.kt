package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.mediator.BehandlingsstatistikkMediator

internal fun Route.behandlingsstatistikkApi(behandlingsstatistikkMediator: BehandlingsstatistikkMediator) {
    get("/api/behandlingsstatistikk") {
        val statistikk = withContext(Dispatchers.IO) {
            behandlingsstatistikkMediator.hentSaksbehandlingsstatistikk()
        }
        call.respond(HttpStatusCode.OK, statistikk)
    }
}
