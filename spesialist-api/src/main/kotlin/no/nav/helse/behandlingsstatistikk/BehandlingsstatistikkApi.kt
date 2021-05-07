package no.nav.helse.behandlingsstatistikk

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.behandlingsstatistikkApi(behandlingsstatistikkMediator: BehandlingsstatistikkMediator) {
    get("/api/behandlingsstatistikk") {
        val statistikk = withContext(Dispatchers.IO) {
            behandlingsstatistikkMediator.hentSaksbehandlingsstatistikk()
        }
        call.respond(HttpStatusCode.OK, statistikk)
    }
}
