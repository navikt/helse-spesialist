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
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.tildeling.TildelingService
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.feilhåndtering.modellfeilForRest
import no.nav.helse.spesialist.api.notat.NyttNotatDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingDto
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TotrinnsvurderingApi")
private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

internal fun Route.totrinnsvurderingApi(
    tildelingService: TildelingService,
    hendelseMediator: HendelseMediator,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    tilgangsgrupper: Tilgangsgrupper,
    saksbehandlerMediator: SaksbehandlerMediator
) {
    post("/api/totrinnsvurdering") {
        val totrinnsvurdering = call.receive<TotrinnsvurderingDto>()
        val saksbehandlerOid = getSaksbehandlerOid()
        val aktivTotrinnsvurdering = totrinnsvurderingMediator.hentAktiv(totrinnsvurdering.oppgavereferanse)
            ?: totrinnsvurderingMediator.opprettFraLegacy(totrinnsvurdering.oppgavereferanse)

        if (aktivTotrinnsvurdering.erBeslutteroppgave()) {
            call.respondText(
                "Denne oppgaven har allerede blitt sendt til godkjenning.",
                status = HttpStatusCode.Conflict
            )
            return@post
        }

        modellfeilForRest {
            saksbehandlerMediator.håndterTotrinnsvurdering(totrinnsvurdering.oppgavereferanse)
            sikkerlogg.info("OppgaveId ${totrinnsvurdering.oppgavereferanse} sendes til godkjenning av $saksbehandlerOid")

            val beslutterSaksbehandlerOid = aktivTotrinnsvurdering.beslutter
            tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
                totrinnsvurdering.oppgavereferanse,
                beslutterSaksbehandlerOid,
                tilganger(tilgangsgrupper),
            )

            totrinnsvurderingMediator.settSaksbehandler(
                oppgaveId = totrinnsvurdering.oppgavereferanse,
                saksbehandlerOid = saksbehandlerOid
            )
            if (aktivTotrinnsvurdering.erRetur) totrinnsvurderingMediator.settHåndtertRetur(totrinnsvurdering.oppgavereferanse)

            totrinnsvurderingMediator.lagrePeriodehistorikk(
                oppgaveId = totrinnsvurdering.oppgavereferanse,
                saksbehandleroid = saksbehandlerOid,
                type = PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING
            )

            hendelseMediator.sendMeldingOppgaveOppdatert(totrinnsvurdering.oppgavereferanse)

            log.info("OppgaveId ${totrinnsvurdering.oppgavereferanse} sendt til godkjenning")

            call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
        }
    }

    post("/api/totrinnsvurdering/retur") {
        val retur = call.receive<TotrinnsvurderingReturDto>()
        val beslutterOid = getSaksbehandlerOid()
        val aktivTotrinnsvurdering = totrinnsvurderingMediator.hentAktiv(oppgaveId = retur.oppgavereferanse) ?:
            totrinnsvurderingMediator.opprettFraLegacy(retur.oppgavereferanse)
        sikkerlogg.info("OppgaveId ${retur.oppgavereferanse} sendes i retur av $beslutterOid")

        val tidligereSaksbehandlerOid = aktivTotrinnsvurdering.saksbehandler

        totrinnsvurderingMediator.settRetur(
            oppgaveId = retur.oppgavereferanse,
            beslutterOid = beslutterOid,
            notat = retur.notat.tekst
        )

        tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
            retur.oppgavereferanse,
            tidligereSaksbehandlerOid,
            tilganger(tilgangsgrupper),
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
class TotrinnsvurderingReturDto(
    val oppgavereferanse: Long,
    val notat: NyttNotatDto,
)
