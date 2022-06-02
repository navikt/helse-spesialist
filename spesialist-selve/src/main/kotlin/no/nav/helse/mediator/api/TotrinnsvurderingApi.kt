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
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.notat.NotatMediator
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.periodehistorikk.PeriodehistorikkType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TotrinnsvurderingApi")
private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

internal fun Route.totrinnsvurderingApi(
    oppgaveMediator: OppgaveMediator,
    periodehistorikkDao: PeriodehistorikkDao,
    notatMediator: NotatMediator,
    tildelingMediator: TildelingMediator,
    hendelseMediator: HendelseMediator,
) {
    post("/api/totrinnsvurdering") {
        val totrinnsvurdering = call.receive<TotrinnsvurderingDto>()
        val saksbehandlerOid = getSaksbehandlerOid()

        sikkerLog.info("OppgaveId ${totrinnsvurdering.oppgavereferanse} sendes til godkjenning av $saksbehandlerOid")

        val tidligereSaksbehandlerOid = oppgaveMediator.finnTidligereSaksbehandler(totrinnsvurdering.oppgavereferanse)
        tildelingMediator.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(totrinnsvurdering.oppgavereferanse, tidligereSaksbehandlerOid)

        oppgaveMediator.setBeslutterOppgave(
            oppgaveId = totrinnsvurdering.oppgavereferanse,
            erBeslutterOppgave = true,
            erReturOppgave = false,
            tidligereSaksbehandlerOid = saksbehandlerOid
        )
        periodehistorikkDao.lagre(PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING, saksbehandlerOid, totrinnsvurdering.periodeId)

        hendelseMediator.sendMeldingOppgaveOppdatert(totrinnsvurdering.oppgavereferanse)

        log.info("OppgaveId ${totrinnsvurdering.oppgavereferanse} sendt til godkjenning")

        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/totrinnsvurdering/retur") {
        val retur = call.receive<TotrinnsvurderingReturDto>()

        val saksbehandlerOid = getSaksbehandlerOid()
        sikkerLog.info("OppgaveId ${retur.oppgavereferanse} sendes i retur av $saksbehandlerOid")

        val tidligereSaksbehandlerOid = oppgaveMediator.finnTidligereSaksbehandler(retur.oppgavereferanse)
        tildelingMediator.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(retur.oppgavereferanse, tidligereSaksbehandlerOid)

        oppgaveMediator.setBeslutterOppgave(
            oppgaveId = retur.oppgavereferanse,
            erBeslutterOppgave = false,
            erReturOppgave = true,
            tidligereSaksbehandlerOid = saksbehandlerOid
        )

        notatMediator.lagreForOppgaveId(retur.oppgavereferanse, retur.notat.tekst, saksbehandlerOid, retur.notat.type)

        hendelseMediator.sendMeldingOppgaveOppdatert(retur.oppgavereferanse)

        log.info("OppgaveId ${retur.oppgavereferanse} sendt i retur")

        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }
}

private fun PipelineContext<Unit, ApplicationCall>.getSaksbehandlerOid(): UUID {
    val accessToken = requireNotNull(call.principal<JWTPrincipal>()) { "mangler access token" }
    return UUID.fromString(accessToken.payload.getClaim("oid").asString())
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
    val notat: NotatApiDto
)
