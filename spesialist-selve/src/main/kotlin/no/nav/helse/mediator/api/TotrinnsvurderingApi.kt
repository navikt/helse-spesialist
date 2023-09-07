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
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.notat.NyttNotatDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingDto
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TotrinnsvurderingApi")
private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

interface Oppgavehåndterer {
    fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: Saksbehandler)
    fun sendIRetur(oppgaveId: Long, besluttendeSaksbehandler: Saksbehandler)
}

internal fun Route.totrinnsvurderingApi(
    hendelseMediator: HendelseMediator,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    saksbehandlerMediator: SaksbehandlerMediator,
    oppgavehåndterer: Oppgavehåndterer
) {
    post("/api/totrinnsvurdering") {
        val totrinnsvurdering = call.receive<TotrinnsvurderingDto>()
        val behandlendeSaksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))
        val saksbehandlerOid = getSaksbehandlerOid()

        saksbehandlerMediator.håndterTotrinnsvurdering(totrinnsvurdering.oppgavereferanse)
        oppgavehåndterer.sendTilBeslutter(totrinnsvurdering.oppgavereferanse, behandlendeSaksbehandler)

        sikkerlogg.info(
            "Oppgave med {} sendes til godkjenning av saksbehandler med {}",
            kv("oppgaveId", totrinnsvurdering.oppgavereferanse),
            kv("oid", saksbehandlerOid),
        )

        totrinnsvurderingMediator.lagrePeriodehistorikk(
            oppgaveId = totrinnsvurdering.oppgavereferanse,
            saksbehandleroid = saksbehandlerOid,
            type = PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING
        )

        hendelseMediator.sendMeldingOppgaveOppdatert(totrinnsvurdering.oppgavereferanse)

        log.info("OppgaveId ${totrinnsvurdering.oppgavereferanse} sendt til godkjenning")

        call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
    }

    post("/api/totrinnsvurdering/retur") {
        val retur = call.receive<TotrinnsvurderingReturDto>()
        val besluttendeSaksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))
        val beslutterOid = getSaksbehandlerOid()

        sikkerlogg.info(
            "Oppgave med {} sendes i retur av beslutter med {}",
            kv("oppgaveId", retur.oppgavereferanse),
            kv("oid", beslutterOid),
        )

        oppgavehåndterer.sendIRetur(retur.oppgavereferanse, besluttendeSaksbehandler)

        totrinnsvurderingMediator.lagrePeriodehistorikk(
            retur.oppgavereferanse,
            beslutterOid,
            PeriodehistorikkType.TOTRINNSVURDERING_RETUR,
            retur.notat.tekst to NotatType.Retur
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
