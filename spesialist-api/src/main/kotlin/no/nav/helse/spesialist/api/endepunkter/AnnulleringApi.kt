package no.nav.helse.spesialist.api.endepunkter

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AnnulleringHandling

fun Route.annulleringApi(saksbehandlerhåndterer: Saksbehandlerhåndterer) {
    post("/api/annullering") {
        val annullering = call.receive<AnnulleringHandling>()
        val saksbehandler = SaksbehandlerFraApi.fraOnBehalfOfToken(requireNotNull(call.principal()))

        saksbehandlerhåndterer.håndter(annullering, saksbehandler)
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}