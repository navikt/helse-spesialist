package no.nav.helse.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.AnnulleringMessage
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import no.nav.helse.vedtaksperiode.VedtaksperiodeMediator
import java.time.LocalDateTime
import java.util.*

internal fun Application.vedtaksperiodeApi(
    vedtaksperiodeMediator: VedtaksperiodeMediator,
    spleisbehovMediator: SpleisbehovMediator
) {
    routing {
        authenticate {
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
                val speilSnapshot = vedtaksperiodeMediator
                    .byggSpeilSnapshotForAktørId(call.parameters["aktørId"]!!)
                if (speilSnapshot == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke vedtaksperiode")
                    return@get
                }
                call.respond(speilSnapshot)
            }
            get("/api/person/fnr/{fødselsnummer}") {
                val speilSnapshot = vedtaksperiodeMediator
                    .byggSpeilSnapshotForFnr(call.parameters["fødselsnummer"]!!)
                if (speilSnapshot == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke vedtaksperiode")
                    return@get
                }
                call.respond(speilSnapshot)
            }
            post("/api/vedtak") {
                val godkjenning = call.receive<Godkjenning>()
                val accessToken = requireNotNull(call.principal<JWTPrincipal>())
                val saksbehandlerIdent = godkjenning.saksbehandlerIdent
                val oid = UUID.fromString(accessToken.payload.getClaim("oid").asString())
                val epostadresse = accessToken.payload.getClaim("preferred_username").asString()

                val løsning = SaksbehandlerLøsning(
                    godkjent = godkjenning.godkjent,
                    godkjenttidspunkt = LocalDateTime.now(),
                    saksbehandlerIdent = saksbehandlerIdent,
                    oid = oid,
                    epostadresse = epostadresse
                )

                spleisbehovMediator.håndter(godkjenning.behovId, løsning)
                call.respond(HttpStatusCode.Created, mapOf("status" to "OK"))
            }
            post("/api/annullering") {
                val saksbehandlerIdent =
                    requireNotNull(call.principal<JWTPrincipal>()).payload.getClaim("NAVident").asString()
                val annullering = call.receive<Annullering>()
                val vedtaksperiodeId = UUID.fromString(annullering.vedtaksperiodeId)

                val message = AnnulleringMessage(
                    aktørId = annullering.aktørId,
                    fødselsnummer = annullering.fødselsnummer,
                    organisasjonsnummer = annullering.organisasjonsnummer,
                    fagsystemId = annullering.fagsystemId,
                    saksbehandlerIdent = saksbehandlerIdent
                )

                spleisbehovMediator.håndter(vedtaksperiodeId, message)
            }
        }
    }
}

@JsonIgnoreProperties
data class Godkjenning(val behovId: UUID, val godkjent: Boolean, val saksbehandlerIdent: String)

@JsonIgnoreProperties
data class Annullering(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val saksbehandlerIdent: String,
    val vedtaksperiodeId: String
)
