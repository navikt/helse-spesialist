package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.notat.NotatMediator
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.periodehistorikk.PeriodehistorikkType

internal fun Route.totrinnsvurderingApi(
    oppgaveMediator: OppgaveMediator,
    periodehistorikkDao: PeriodehistorikkDao,
    notatMediator: NotatMediator,
    tildelingMediator: TildelingMediator
) {
    post("/api/totrinnsvurdering") {
        val totrinnsvurdering = call.receive<TotrinnsvurderingDto>()

        val saksbehandlerOid = getSaksbehandlerOid()
        val tidligereSaksbehandlerOid = oppgaveMediator.finnTidligereSaksbehandler(totrinnsvurdering.oppgavereferanse)
        tildelingMediator.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(totrinnsvurdering.oppgavereferanse, tidligereSaksbehandlerOid)

        oppgaveMediator.setBeslutterOppgave(
            oppgaveId = totrinnsvurdering.oppgavereferanse,
            erBeslutterOppgave = true,
            erReturOppgave = false,
            tidligereSaksbehandlerOid = saksbehandlerOid
        )
        periodehistorikkDao.lagre(PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING, saksbehandlerOid, totrinnsvurdering.periodeId)

        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/totrinnsvurdering/retur") {
        val retur = call.receive<TotrinnsvurderingReturDto>()

        val saksbehandlerOid = getSaksbehandlerOid()
        val tidligereSaksbehandlerOid = oppgaveMediator.finnTidligereSaksbehandler(retur.oppgavereferanse)
        tildelingMediator.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(retur.oppgavereferanse, tidligereSaksbehandlerOid)

        oppgaveMediator.setBeslutterOppgave(
            oppgaveId = retur.oppgavereferanse,
            erBeslutterOppgave = false,
            erReturOppgave = true,
            tidligereSaksbehandlerOid = saksbehandlerOid
        )

        val notatId = retur.notat?.let { notatMediator.lagreForOppgaveId(retur.oppgavereferanse, it, saksbehandlerOid) }
        periodehistorikkDao.lagre(PeriodehistorikkType.TOTRINNSVURDERING_RETUR, saksbehandlerOid, retur.periodeId, notatId?.toInt())

        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}

private fun PipelineContext<Unit, ApplicationCall>.getSaksbehandlerOid(): UUID {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return UUID.fromString(accessToken.payload.getClaim("oid").asString())
}

internal fun PipelineContext<Unit, ApplicationCall>.getGrupper(): List<UUID> {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return accessToken.payload.getClaim("groups").asList(String::class.java).map(UUID::fromString)
}

@JsonIgnoreProperties
class TotrinnsvurderingDto(
    val oppgavereferanse: Long,
    val periodeId: UUID,
)

@JsonIgnoreProperties
class TotrinnsvurderingReturDto(
    val oppgavereferanse: Long,
    val periodeId: UUID,
    val notat: String? = null
)
