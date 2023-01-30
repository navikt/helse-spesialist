package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID
import no.nav.helse.gruppemedlemskap
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.tildeling.TildelingService
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TotrinnsvurderingApi")
private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

internal fun Route.totrinnsvurderingApi(
    varselRepository: ApiVarselRepository,
    oppgaveMediator: OppgaveMediator,
    periodehistorikkDao: PeriodehistorikkDao,
    notatMediator: NotatMediator,
    tildelingService: TildelingService,
    hendelseMediator: HendelseMediator,
) {
    post("/api/totrinnsvurdering") {
        val totrinnsvurdering = call.receive<TotrinnsvurderingDto>()
        val saksbehandlerOid = getSaksbehandlerOid()

        if (oppgaveMediator.erBeslutteroppgave(totrinnsvurdering.oppgavereferanse)) {
            call.respondText("Denne oppgaven har allerede blitt sendt til godkjenning.",
                status = HttpStatusCode.Conflict
            )
            return@post
        }

        val antallIkkeVurderteVarsler = varselRepository.ikkeVurderteVarslerEkskludertBesluttervarslerFor(totrinnsvurdering.oppgavereferanse)
        if (antallIkkeVurderteVarsler > 0) {
            call.respond(
                status = HttpStatusCode.BadRequest,
                mapOf(
                    "melding" to "Alle varsler må vurderes før godkjenning - $antallIkkeVurderteVarsler varsler er ikke vurdert",
                    "feilkode" to "IkkeVurderteVarslerVedGodkjenning"
                )
            )
            return@post
        }

        sikkerLog.info("OppgaveId ${totrinnsvurdering.oppgavereferanse} sendes til godkjenning av $saksbehandlerOid")

        val beslutterSaksbehandlerOid = oppgaveMediator.finnBeslutterSaksbehandler(totrinnsvurdering.oppgavereferanse)
        tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(totrinnsvurdering.oppgavereferanse, beslutterSaksbehandlerOid, gruppemedlemskap())

        oppgaveMediator.setBeslutteroppgave(
            oppgaveId = totrinnsvurdering.oppgavereferanse,
            tidligereSaksbehandlerOid = saksbehandlerOid
        )

        oppgaveMediator.lagrePeriodehistorikk(totrinnsvurdering.oppgavereferanse,
            periodehistorikkDao,
            saksbehandlerOid,
            PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING
        )

        hendelseMediator.sendMeldingOppgaveOppdatert(totrinnsvurdering.oppgavereferanse)

        log.info("OppgaveId ${totrinnsvurdering.oppgavereferanse} sendt til godkjenning")

        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/totrinnsvurdering/retur") {
        val retur = call.receive<TotrinnsvurderingReturDto>()

        val saksbehandlerOid = getSaksbehandlerOid()
        sikkerLog.info("OppgaveId ${retur.oppgavereferanse} sendes i retur av $saksbehandlerOid")

        val tidligereSaksbehandlerOid = oppgaveMediator.finnTidligereSaksbehandler(retur.oppgavereferanse)

        oppgaveMediator.setReturoppgave(
            oppgaveId = retur.oppgavereferanse,
            beslutterSaksbehandlerOid = saksbehandlerOid
        )

        tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(retur.oppgavereferanse, tidligereSaksbehandlerOid, gruppemedlemskap())

        val notatId = notatMediator.lagreForOppgaveId(retur.oppgavereferanse, retur.notat.tekst, saksbehandlerOid, retur.notat.type)

        oppgaveMediator.lagrePeriodehistorikk(
            retur.oppgavereferanse, periodehistorikkDao,
            saksbehandlerOid,
            PeriodehistorikkType.TOTRINNSVURDERING_RETUR,
            notatId?.toInt()
        )

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
)

@JsonIgnoreProperties
class TotrinnsvurderingReturDto(
    val oppgavereferanse: Long,
    val notat: NotatApiDto
)
