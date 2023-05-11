package no.nav.helse.spesialist.api.endepunkter

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.utbetaling.AnnulleringDto
import no.nav.helse.spesialist.api.utbetaling.AnnulleringKafkaDto

fun Route.annulleringApi(saksbehandlerMediator: SaksbehandlerMediator) {
    post("/api/annullering") {
        val annullering = call.receive<AnnulleringDto>()
        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))

        val message = AnnulleringKafkaDto(
            saksbehandler = saksbehandler,
            organisasjonsnummer = annullering.organisasjonsnummer,
            fødselsnummer = annullering.fødselsnummer,
            aktørId = annullering.aktørId,
            fagsystemId = annullering.fagsystemId,
            begrunnelser = annullering.begrunnelser,
            kommentar = annullering.kommentar
        )

        saksbehandlerMediator.håndter(message, saksbehandler)
        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}