package no.nav.helse.mediator.api

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.notat.NotatMediator
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.helse.notat.NotatType

private val log = LoggerFactory.getLogger("NotatApi")

internal fun Route.notaterApi(mediator: NotatMediator) {

    post("/api/notater/{vedtaksperiode_id}") {
        val notat = call.receive<NotatApiDto>()

        val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
        val saksbehandlerOid = UUID.fromString(accessToken.payload.getClaim("oid").asString())

        val vedtaksperiodeId = UUID.fromString(call.parameters["vedtaksperiode_id"])
        if (vedtaksperiodeId == null) {
            call.respond(HttpStatusCode.BadRequest, "vedtaksperiode_id er ikke oppgitt")
            log.warn("POST - vedtaksperiode_id er null i path parameter. Saksbehandler OID: $saksbehandlerOid")
            return@post
        }
        withContext(Dispatchers.IO) {
            mediator.lagre(vedtaksperiodeId, notat.tekst, saksbehandlerOid, notat.type)
        }
        call.respond(HttpStatusCode.OK)
    }

    get("/api/notater") {
        val vedtaksperiodeIds = call.request.queryParameters
            .entries()
            .filter { it.key == "vedtaksperiode_id" }
            .flatMap { it.value }
            .map(UUID::fromString)

        if (vedtaksperiodeIds.isNotEmpty()) {
            val notater = withContext(Dispatchers.IO) { mediator.finn(vedtaksperiodeIds) }
            call.respond(notater)
        } else {
            val accessToken = checkNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
            val saksbehandler_oid = UUID.fromString(accessToken.payload.getClaim("oid").asString())

            call.respond(HttpStatusCode.BadRequest, "vedtaksperiode_id er ikke oppgitt")
            log.warn("GET - vedtaksperiode_id mangler i path parameter. Saksbehandler OID: $saksbehandler_oid")
        }
    }

    put("/api/notater/{vedtaksperiode_id}/feilregistrer/{notat_id}") {
        val notatId = call.parameters["notat_id"]!!.let {
            requireNotNull(it.toIntOrNull()) { "$it er ugyldig notat_id i path parameter" }
        }
        call.parameters["vedtaksperiode_id"]!!.let {
            requireNotNull(UUID.fromString(it)) { "$it er ugyldig vedtaksperiode_id i path parameter" }
        }

        val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
        val saksbehandler_oid = UUID.fromString(accessToken.payload.getClaim("oid").asString())

        val feilregistrering = withContext(Dispatchers.IO) {
            mediator.feilregistrer(notatId, saksbehandler_oid)
        }
        if (!feilregistrering) {
            call.respond(HttpStatusCode.BadRequest, "Kan ikke feilregistrere det notatet")
        }
        else call.respond(HttpStatusCode.OK)
    }
}

data class NotatApiDto(
    val tekst: String,
    val type: NotatType
)

