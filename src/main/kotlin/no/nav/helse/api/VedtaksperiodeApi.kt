package no.nav.helse.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.meldinger.AnnulleringMessage
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.command.findNåværendeOppgave
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.vedtaksperiode.VedtaksperiodeMediator
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal fun Application.vedtaksperiodeApi(
    vedtaksperiodeMediator: VedtaksperiodeMediator,
    spleisbehovMediator: HendelseMediator,
    dataSource: DataSource
) {
    routing {
        authenticate("saksbehandler") {
            get("/api/person/{vedtaksperiodeId}") {
                val speilSnapshot = vedtaksperiodeMediator
                    .byggSpeilSnapshotForVedtaksperiodeId(UUID.fromString(call.parameters["vedtaksperiodeId"]!!))
                if (speilSnapshot == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke vedtaksperiode")
                    return@get
                }
                call.respond(speilSnapshot)
            }
            get("/api/person/aktorId/{aktørId}") {
                call.parameters["aktørId"]?.toLongOrNull() ?: run {
                    call.respond(status = HttpStatusCode.BadRequest, message = "AktørId må være numerisk")
                    return@get
                }
                val speilSnapshot = vedtaksperiodeMediator
                    .byggSpeilSnapshotForAktørId(call.parameters["aktørId"]!!)
                if (speilSnapshot == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke vedtaksperiode")
                    return@get
                }
                call.respond(speilSnapshot)
            }
            get("/api/person/fnr/{fødselsnummer}") {
                call.parameters["fødselsnummer"]?.toLongOrNull() ?: run {
                    call.respond(status = HttpStatusCode.BadRequest, message = "Fødselsnummer må være numerisk")
                    return@get
                }
                val speilSnapshot = vedtaksperiodeMediator
                    .byggSpeilSnapshotForFnr(call.parameters["fødselsnummer"]!!)
                if (speilSnapshot == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke vedtaksperiode")
                    return@get
                }
                call.respond(speilSnapshot)
            }
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
                val godkjenning = call.receive<Godkjenning>()
                val accessToken = requireNotNull(call.principal<JWTPrincipal>())
                val saksbehandlerIdent = godkjenning.saksbehandlerIdent
                val oid = UUID.fromString(accessToken.payload.getClaim("oid").asString())
                val epostadresse = accessToken.payload.getClaim("preferred_username").asString()

                val oppgave =
                    using(sessionOf(dataSource)) { session -> session.findNåværendeOppgave(godkjenning.oppgavereferanse) }
                if (oppgave == null) {
                    call.respondText(
                        "Dette vedtaket har ingen aktiv saksbehandleroppgave. Dette betyr vanligvis at oppgaven allerede er fullført.",
                        status = HttpStatusCode.Conflict
                    )
                    return@post
                }

                validerSaksbehandlerInput(godkjenning)

                val løsning = SaksbehandlerLøsning(
                    godkjent = godkjenning.godkjent,
                    godkjenttidspunkt = LocalDateTime.now(),
                    saksbehandlerIdent = saksbehandlerIdent,
                    oid = oid,
                    epostadresse = epostadresse,
                    årsak = godkjenning.årsak,
                    begrunnelser = godkjenning.begrunnelser,
                    kommentar = godkjenning.kommentar
                )

                spleisbehovMediator.håndter(godkjenning.oppgavereferanse, løsning)
                call.respond(HttpStatusCode.Created, mapOf("status" to "OK"))
            }
            post("/api/annullering") {
                val annullering = call.receive<Annullering>()
                val accessToken = requireNotNull(call.principal<JWTPrincipal>())
                val epostadresse = accessToken.payload.getClaim("preferred_username").asString()

                val message = AnnulleringMessage(
                    aktørId = annullering.aktørId,
                    fødselsnummer = annullering.fødselsnummer,
                    organisasjonsnummer = annullering.organisasjonsnummer,
                    fagsystemId = annullering.fagsystemId,
                    saksbehandler = annullering.saksbehandlerIdent,
                    saksbehandlerEpost = epostadresse
                )

                spleisbehovMediator.håndter(message)
                call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
            }

            post("/api/overstyr/dager") {
                val overstyring = call.receive<Overstyring>()

                val accessToken = requireNotNull(call.principal<JWTPrincipal>())
                val oid = UUID.fromString(accessToken.payload.getClaim("oid").asString())
                val epostadresse = accessToken.payload.getClaim("preferred_username").asString()
                val saksbehandlerNavn = accessToken.payload.getClaim("name").asString()

                val message = OverstyringMessage(
                    saksbehandlerEpost = epostadresse,
                    saksbehandlerOid = oid,
                    saksbehandlerNavn = saksbehandlerNavn,
                    organisasjonsnummer = overstyring.organisasjonsnummer,
                    fødselsnummer = overstyring.fødselsnummer,
                    aktørId = overstyring.aktørId,
                    begrunnelse = overstyring.begrunnelse,
                    dager = overstyring.dager.map {
                        OverstyringMessage.OverstyringMessageDag(
                            dato = it.dato,
                            type = enumValueOf(it.type),
                            grad = it.grad
                        )
                    },
                    unntaFraInnsyn = overstyring.unntaFraInnsyn
                )

                spleisbehovMediator.håndter(message)
                call.respond(HttpStatusCode.OK, mapOf("status" to "OK"))
            }
        }
    }
}

fun validerSaksbehandlerInput(godkjenning: Godkjenning) {
    if (!godkjenning.godkjent) {
        requireNotNull(godkjenning.årsak)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Godkjenning(
    val oppgavereferanse: UUID,
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?
)

@JsonIgnoreProperties
data class Annullering(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val saksbehandlerIdent: String,
    val vedtaksperiodeId: String
)

@JsonIgnoreProperties
class Overstyring(
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<Overstyringdag>,
    val unntaFraInnsyn: Boolean
) {
    class Overstyringdag(
        val dato: LocalDate,
        val type: String,
        val grad: Int?
    )
}
