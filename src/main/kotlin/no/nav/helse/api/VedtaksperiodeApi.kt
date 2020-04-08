package no.nav.helse.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.dto.ArbeidsgiverForSpeilDto
import no.nav.helse.modell.dto.PersonDTO
import no.nav.helse.modell.dto.PersonForSpeilDto
import no.nav.helse.objectMapper
import java.util.*

internal fun Application.vedtaksperiodeApi(
    personDao: PersonDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    arbeidsgiverDao: ArbeidsgiverDao
) {
    routing {
        authenticate {
            get("/api/vedtaksperiode/{vedtaksperiodeId}") {
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
                        .let { objectMapper.readValue<PersonDTO>(it) }
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
        }
    }
}
