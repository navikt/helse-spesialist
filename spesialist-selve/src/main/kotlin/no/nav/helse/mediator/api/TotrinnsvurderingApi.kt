package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.periodehistorikk.PeriodehistorikkType

internal fun Route.totrinnsvurderingApi(oppgaveMediator: OppgaveMediator, periodehistorikkDao: PeriodehistorikkDao) {
    post("/api/totrinnsvurdering") {
        val totrinnsvurdering = call.receive<TotrinnsvurderingDto>()

        val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
        val saksbehandlerOid = UUID.fromString(accessToken.payload.getClaim("oid").asString())

        oppgaveMediator.setBeslutterOppgave(
            oppgaveId = totrinnsvurdering.oppgavereferanse,
            erBeslutterOppgave = true,
            erReturOppgave = false,
            tidligereSaksbehandlerOid = saksbehandlerOid
        )
        periodehistorikkDao.lagre(PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING, saksbehandlerOid, totrinnsvurdering.periodeId)

        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}

@JsonIgnoreProperties
class TotrinnsvurderingDto(
    val oppgavereferanse: Long,
    val periodeId: UUID,
)
