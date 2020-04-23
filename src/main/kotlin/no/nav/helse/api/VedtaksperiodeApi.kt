package no.nav.helse.api

import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.dto.*
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import no.nav.helse.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal fun Application.vedtaksperiodeApi(
    personDao: PersonDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    spleisbehovMediator: SpleisbehovMediator
) {
    val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    routing {
        authenticate {
            get("/api/person/{vedtaksperiodeId}") {
                val vedtaksperiodeId = UUID.fromString(call.parameters["vedtaksperiodeId"]!!)
                val vedtak = vedtakDao.findVedtakByVedtaksperiodeId(vedtaksperiodeId)
                if (vedtak == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Fant ikke vedtaksperiode"
                    )
                    return@get
                }
                val personDto = personDao.findPerson(vedtak.personRef)
                if (personDto == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Fant ikke person"
                    )
                    return@get
                }
                val arbeidsgiverDto =
                    requireNotNull(arbeidsgiverDao.findArbeidsgiver(vedtak.arbeidsgiverRef)) { "Fant ikke arbeidsgiver" }
                val speilSnapshot =
                    requireNotNull(snapshotDao.findSpeilSnapshot(vedtak.speilSnapshotRef)) { "Fant ikke speilSnapshot" }
                        .let { objectMapper.readValue<PersonFraSpleisDto>(it) }
                val arbeidsgivere = speilSnapshot.arbeidsgivere.map {
                    if (it.organisasjonsnummer == arbeidsgiverDto.organisasjonsnummer) ArbeidsgiverForSpeilDto(
                        it.organisasjonsnummer,
                        arbeidsgiverDto.navn,
                        it.id,
                        it.vedtaksperioder
                    ) else ArbeidsgiverForSpeilDto(
                        it.organisasjonsnummer,
                        "Ikke tilgjengelig",
                        it.id,
                        it.vedtaksperioder
                    )
                }
                val personForSpeil = PersonForSpeilDto(
                    speilSnapshot.aktørId,
                    speilSnapshot.fødselsnummer,
                    personDto.navn,
                    arbeidsgivere
                )
                call.respond(personForSpeil)
            }
            get("/api/person/aktorId/{aktørId}") {
                val aktørId = call.parameters["aktørId"]!!.toLong()
                val personId = personDao.findPersonByAktørId(aktørId)
                if (personId == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Fant ikke person"
                    )
                    return@get
                }
                val vedtak = vedtakDao.findVedtakByPersonRef(personId)
                if (vedtak == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Fant ikke vedtaksperiode"
                    )
                    return@get
                }
                val arbeidsgiverDto =
                    requireNotNull(arbeidsgiverDao.findArbeidsgiver(vedtak.arbeidsgiverRef)) { "Fant ikke arbeidsgiver" }
                val speilSnapshot =
                    requireNotNull(snapshotDao.findSpeilSnapshot(vedtak.speilSnapshotRef)) { "Fant ikke speilSnapshot" }
                        .let { objectMapper.readValue<PersonFraSpleisDto>(it) }
                val arbeidsgivere = speilSnapshot.arbeidsgivere.map {
                    if (it.organisasjonsnummer == arbeidsgiverDto.organisasjonsnummer) ArbeidsgiverForSpeilDto(
                        it.organisasjonsnummer,
                        arbeidsgiverDto.navn,
                        it.id,
                        it.vedtaksperioder
                    ) else ArbeidsgiverForSpeilDto(
                        it.organisasjonsnummer,
                        "Ikke tilgjengelig",
                        it.id,
                        it.vedtaksperioder
                    )
                }
                val navnDto = requireNotNull(personDao.findNavn(personId)){ "Fant ikke navn for person" }
                val personForSpeil = PersonForSpeilDto(
                    speilSnapshot.aktørId,
                    speilSnapshot.fødselsnummer,
                    navnDto,
                    arbeidsgivere
                )
                call.respond(personForSpeil)
            }
            get("/api/person/fnr/{fødselsnummer}") {
                val fødselsnummer = call.parameters["fødselsnummer"]!!.toLong()
                val personId = personDao.findPersonByFødselsnummer(fødselsnummer)
                if (personId == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Fant ikke person"
                    )
                    return@get
                }
                val vedtak = vedtakDao.findVedtakByPersonRef(personId)
                if (vedtak == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Fant ikke vedtaksperiode"
                    )
                    return@get
                }
                val arbeidsgiverDto =
                    requireNotNull(arbeidsgiverDao.findArbeidsgiver(vedtak.arbeidsgiverRef)) { "Fant ikke arbeidsgiver" }
                val speilSnapshot =
                    requireNotNull(snapshotDao.findSpeilSnapshot(vedtak.speilSnapshotRef)) { "Fant ikke speilSnapshot" }
                        .let { objectMapper.readValue<PersonFraSpleisDto>(it) }
                val arbeidsgivere = speilSnapshot.arbeidsgivere.map {
                    if (it.organisasjonsnummer == arbeidsgiverDto.organisasjonsnummer) ArbeidsgiverForSpeilDto(
                        it.organisasjonsnummer,
                        arbeidsgiverDto.navn,
                        it.id,
                        it.vedtaksperioder
                    ) else ArbeidsgiverForSpeilDto(
                        it.organisasjonsnummer,
                        "Ikke tilgjengelig",
                        it.id,
                        it.vedtaksperioder
                    )
                }
                val navnDto = requireNotNull(personDao.findNavn(personId)){ "Fant ikke navn for person" }
                val personForSpeil = PersonForSpeilDto(
                    speilSnapshot.aktørId,
                    speilSnapshot.fødselsnummer,
                    navnDto,
                    arbeidsgivere
                )
                call.respond(personForSpeil)
            }
            post("/api/vedtak") {
                val godkjenning = call.receive<Godkjenning>()
                val accessToken = requireNotNull(call.principal<JWTPrincipal>())
                val saksbehandlerIdent = accessToken.payload.getClaim("NAVident").asString()

                sikkerLogg.info("Token: " + call.request.parseAuthorizationHeader())

                val løsning = SaksbehandlerLøsning(
                    godkjent = godkjenning.godkjent,
                    godkjenttidspunkt = LocalDateTime.now(),
                    saksbehandlerIdent = saksbehandlerIdent
                )

                spleisbehovMediator.håndter(godkjenning.behovId, løsning)
                call.respond(HttpStatusCode.Created, mapOf("status" to "OK"))
            }

        }
    }
}


@JsonIgnoreProperties
data class Godkjenning(val behovId: UUID, val godkjent: Boolean)
