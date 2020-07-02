package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import kotliquery.sessionOf
import no.nav.helse.measureAsHistogram
import no.nav.helse.modell.arbeidsgiver.findArbeidsgiver
import no.nav.helse.modell.command.eventIdForVedtaksperiode
import no.nav.helse.modell.person.findEnhet
import no.nav.helse.modell.person.findInfotrygdutbetalinger
import no.nav.helse.modell.vedtak.NavnDto
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import no.nav.helse.modell.vedtak.snapshot.findSpeilSnapshot
import no.nav.helse.objectMapper
import java.util.*
import javax.sql.DataSource

internal class VedtaksperiodeMediator(val dataSource: DataSource) {
    fun byggSpeilSnapshotForFnr(fnr: String) =
        measureAsHistogram("byggSpeilSnapshotForFnr") {
            sessionOf(dataSource).use { session ->
                session.findVedtakByFnr(fnr)?.let { byggSpeilSnapshot(session, it) }
            }
        }

    fun byggSpeilSnapshotForAktørId(aktørId: String) =
        measureAsHistogram("byggSpeilSnapshotForAktørId") {
            sessionOf(dataSource).use { session ->
                session.findVedtakByAktørId(aktørId)?.let { byggSpeilSnapshot(session, it) }
            }
        }

    fun byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeId: UUID) =
        measureAsHistogram("byggSpeilSnapshotForVedtaksperiodeId") {
            sessionOf(dataSource).use { session ->
                session.findVedtakByVedtaksperiodeId(vedtaksperiodeId)?.let { byggSpeilSnapshot(session, it) }
            }
        }

    private fun byggSpeilSnapshot(session: Session, vedtak: VedtaksperiodeDto) =
        measureAsHistogram("byggSpeilSnapshot") {
            val arbeidsgiverDto = measureAsHistogram("byggSpeilSnapshot_findArbeidsgiver") {
                requireNotNull(session.findArbeidsgiver(vedtak.arbeidsgiverRef)) { "Fant ikke arbeidsgiver" }
            }
            val infotrygdutbetalinger = measureAsHistogram("byggSpeilSnapshot_findInfotrygdutbetalinger") {
                session.findInfotrygdutbetalinger(vedtak.fødselsnummer.toLong())?.let { objectMapper.readTree(it) }
            }
            val speilSnapshot = measureAsHistogram("byggSpeilSnapshot_findSpeilSnapshot") {
                requireNotNull(session.findSpeilSnapshot(vedtak.speilSnapshotRef)) { "Fant ikke speilSnapshot" }
                    .let { objectMapper.readValue<PersonFraSpleisDto>(it) }
            }
            val arbeidsgivere = speilSnapshot.arbeidsgivere.map {
                val arbeidsgivernavn =
                    if (it.organisasjonsnummer == arbeidsgiverDto.organisasjonsnummer)
                        arbeidsgiverDto.navn
                    else
                        "Ikke tilgjengelig"
                ArbeidsgiverForSpeilDto(
                    organisasjonsnummer = it.organisasjonsnummer,
                    navn = arbeidsgivernavn,
                    id = it.id,
                    vedtaksperioder = it.vedtaksperioder
                )
            }
            measureAsHistogram("byggSpeilSnapshot_behovForVedtaksperiode_akkumulert") {
                speilSnapshot.arbeidsgivere.forEach { arbeidsgiver ->
                    arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                        val vedtaksperiodeId = UUID.fromString(vedtaksperiode["id"].asText())
                        val eventId = session.eventIdForVedtaksperiode(vedtaksperiodeId)
                        vedtaksperiode as ObjectNode
                        vedtaksperiode.put("oppgavereferanse", eventId?.toString())
                    }
                }
            }

            val enhet = measureAsHistogram("byggSpeilSnapshot_findEnhet") {
                session.findEnhet(vedtak.fødselsnummer.toLong())
            }
            PersonForSpeilDto(
                aktørId = speilSnapshot.aktørId,
                fødselsnummer = speilSnapshot.fødselsnummer,
                navn = NavnDto(vedtak.fornavn, vedtak.mellomnavn, vedtak.etternavn),
                arbeidsgivere = arbeidsgivere,
                infotrygdutbetalinger = infotrygdutbetalinger,
                enhet = enhet
            )
        }
}
