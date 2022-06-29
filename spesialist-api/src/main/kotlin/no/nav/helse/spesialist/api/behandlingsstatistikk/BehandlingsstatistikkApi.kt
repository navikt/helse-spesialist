package no.nav.helse.spesialist.api.behandlingsstatistikk

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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
