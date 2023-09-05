package no.nav.helse.mediator.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.Gruppe
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.feilhåndtering.IkkeTilgangTilRiskQa
import no.nav.helse.spesialist.api.feilhåndtering.IkkeÅpenOppgave
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.slf4j.LoggerFactory

internal fun Route.personApi(
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    oppdaterPersonService: OppdaterPersonService,
    godkjenningService: GodkjenningService,
    oppgaveDao: OppgaveDao,
    tilgangsgrupper: Tilgangsgrupper,
    saksbehandlerMediator: SaksbehandlerMediator
) {
    val logg = LoggerFactory.getLogger("PersonApi")
    val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    get("/api/person/aktorId/{aktørId}/sist_oppdatert") {
        call.parameters["aktørId"]?.toLongOrNull() ?: run {
            call.respond(status = HttpStatusCode.BadRequest, message = "AktørId må være numerisk")
            return@get
        }
    }

    get("/api/person/fnr/{fødselsnummer}/sist_oppdatert") {
        call.parameters["fødselsnummer"]?.toLongOrNull() ?: run {
            call.respond(status = HttpStatusCode.BadRequest, message = "Fødselsnummer må være numerisk")
            return@get
        }
    }

    post("/api/vedtak") {
        val godkjenning = call.receive<GodkjenningDto>()
        val saksbehandler = Saksbehandler.fraOnBehalfOfToken(requireNotNull(call.principal()))
        logg.info("Behandler godkjenning/avslag: ${godkjenning.åpenLoggString()} (se sikker logg for detaljer)")
        sikkerlogg.info("Behandler godkjenning/avslag: $godkjenning")
        val (oid, epostadresse) = requireNotNull(call.principal<JWTPrincipal>()).payload.let {
            UUID.fromString(it.getClaim("oid").asString()) to it.getClaim("preferred_username").asString()
        }

        val erÅpenOppgave =
            withContext(Dispatchers.IO) { oppgaveDao.venterPåSaksbehandler(godkjenning.oppgavereferanse) }
        if (!erÅpenOppgave) {
            throw IkkeÅpenOppgave(godkjenning.saksbehandlerIdent, godkjenning.oppgavereferanse)
        }

        val tilgangskontroll = tilganger(tilgangsgrupper)
        val erRiskOppgave = withContext(Dispatchers.IO) { oppgaveDao.erRiskoppgave(godkjenning.oppgavereferanse) }
        if (erRiskOppgave && !tilgangskontroll.harTilgangTil(Gruppe.RISK_QA)) {
            throw IkkeTilgangTilRiskQa(godkjenning.saksbehandlerIdent, godkjenning.oppgavereferanse)
        }

        val totrinnsvurdering = totrinnsvurderingMediator.hentAktiv(godkjenning.oppgavereferanse)

        if (totrinnsvurdering?.erBeslutteroppgave() == true) {
            if (!tilgangskontroll.harTilgangTil(Gruppe.BESLUTTER) && !erDev()) {
                call.respondText(
                    "Saksbehandler trenger beslutter-rolle for å kunne utbetale beslutteroppgaver",
                    status = HttpStatusCode.Unauthorized
                )
                return@post
            }

            if (totrinnsvurdering.saksbehandler == oid && !erDev()) {
                call.respondText(
                    "Kan ikke beslutte egne oppgaver.",
                    status = HttpStatusCode.Unauthorized
                )
                return@post
            }

            totrinnsvurderingMediator.settBeslutter(totrinnsvurdering.vedtaksperiodeId, oid)
        }

        val behandlingId = UUID.randomUUID()

        saksbehandlerMediator.håndter(godkjenning, behandlingId, saksbehandler)
        withContext(Dispatchers.IO) { godkjenningService.håndter(godkjenning, epostadresse, oid, behandlingId) }
        call.respond(HttpStatusCode.Created, mapOf("status" to "OK"))
    }

    post("/api/person/oppdater") {
        val personoppdateringDto = call.receive<OppdaterPersonsnapshotDto>()
        oppdaterPersonService.håndter(personoppdateringDto)
        call.respond(HttpStatusCode.OK)
    }
}

fun erDev() = "dev-gcp" == System.getenv("NAIS_CLUSTER_NAME")
fun erProd() = "prod-gcp" == System.getenv("NAIS_CLUSTER_NAME")

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppdaterPersonsnapshotDto(
    val fødselsnummer: String,
)

