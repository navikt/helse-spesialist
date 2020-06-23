package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.measureAsHistogram
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.eventIdForVedtaksperiode
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.NavnDto
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.objectMapper
import java.util.*
import javax.sql.DataSource

internal class VedtaksperiodeMediator(
    private val vedtaksperiodeDao: VedtaksperiodeDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val snapshotDao: SnapshotDao,
    private val personDao: PersonDao,
    private val dataSource: DataSource
) {
    fun byggSpeilSnapshotForFnr(fnr: String) =
        measureAsHistogram("byggSpeilSnapshotForFnr") { vedtaksperiodeDao.findVedtakByFnr(fnr) }?.let(::byggSpeilSnapshot)

    fun byggSpeilSnapshotForAktørId(aktørId: String) =
        measureAsHistogram("byggSpeilSnapshotForAktørId") { vedtaksperiodeDao.findVedtakByAktørId(aktørId) }?.let(::byggSpeilSnapshot)

    fun byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeId: UUID) =
        measureAsHistogram("byggSpeilSnapshotForVedtaksperiodeId") {
            vedtaksperiodeDao.findVedtakByVedtaksperiodeId(vedtaksperiodeId)
        }?.let(::byggSpeilSnapshot)

    private fun byggSpeilSnapshot(vedtak: VedtaksperiodeDto) = measureAsHistogram("byggSpeilSnapshot") {
        using(sessionOf(dataSource)) { session ->
            val arbeidsgiverDto = measureAsHistogram("byggSpeilSnapshot_findArbeidsgiver") {
                requireNotNull(arbeidsgiverDao.findArbeidsgiver(vedtak.arbeidsgiverRef)) { "Fant ikke arbeidsgiver" }
            }
            val infotrygdutbetalinger = measureAsHistogram("byggSpeilSnapshot_findInfotrygdutbetalinger") {
                personDao.findInfotrygdutbetalinger(vedtak.fødselsnummer.toLong())?.let { objectMapper.readTree(it) }
            }
            val speilSnapshot = measureAsHistogram("byggSpeilSnapshot_findSpeilSnapshot") {
                requireNotNull(snapshotDao.findSpeilSnapshot(vedtak.speilSnapshotRef)) { "Fant ikke speilSnapshot" }
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
                personDao.findEnhet(vedtak.fødselsnummer.toLong())
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
}
