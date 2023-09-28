package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

internal fun Route.personApi(
    oppdaterPersonService: OppdaterPersonService
) {

    post("/api/person/oppdater") {
        val personoppdateringDto = call.receive<OppdaterPersonsnapshotDto>()
        oppdaterPersonService.håndter(personoppdateringDto)
        call.respond(HttpStatusCode.OK)
    }
}

fun erProd() = "prod-gcp" == System.getenv("NAIS_CLUSTER_NAME")

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppdaterPersonsnapshotDto(
    val fødselsnummer: String,
)

