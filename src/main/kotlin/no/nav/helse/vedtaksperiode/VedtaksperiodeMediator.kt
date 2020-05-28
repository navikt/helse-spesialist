package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.behovForVedtaksperide
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
    fun byggSpeilSnapshotForFnr(fnr: String) = vedtaksperiodeDao.findVedtakByFnr(fnr)?.let(::byggSpeilSnapshot)
    fun byggSpeilSnapshotForAktørId(aktørId: String) =
        vedtaksperiodeDao.findVedtakByAktørId(aktørId)?.let(::byggSpeilSnapshot)

    fun byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeId: UUID) =
        vedtaksperiodeDao.findVedtakByVedtaksperiodeId(vedtaksperiodeId)?.let(::byggSpeilSnapshot)

    private fun byggSpeilSnapshot(vedtak: VedtaksperiodeDto) = using(sessionOf(dataSource)) { session ->
        val arbeidsgiverDto =
            requireNotNull(arbeidsgiverDao.findArbeidsgiver(vedtak.arbeidsgiverRef)) { "Fant ikke arbeidsgiver" }
        val infotrygdutbetalinger =
            personDao.findInfotrygdutbetalinger(vedtak.fødselsnummer.toLong())?.let { objectMapper.readTree(it) }
        val speilSnapshot =
            requireNotNull(snapshotDao.findSpeilSnapshot(vedtak.speilSnapshotRef)) { "Fant ikke speilSnapshot" }
                .let { objectMapper.readValue<PersonFraSpleisDto>(it) }
        val arbeidsgivere = speilSnapshot.arbeidsgivere.map {
            if (it.organisasjonsnummer == arbeidsgiverDto.organisasjonsnummer) ArbeidsgiverForSpeilDto(
                organisasjonsnummer = it.organisasjonsnummer,
                navn = arbeidsgiverDto.navn,
                id = it.id,
                vedtaksperioder = it.vedtaksperioder
            ) else ArbeidsgiverForSpeilDto(
                organisasjonsnummer = it.organisasjonsnummer,
                navn = "Ikke tilgjengelig",
                id = it.id,
                vedtaksperioder = it.vedtaksperioder
            )
        }
        speilSnapshot.arbeidsgivere.forEach { arbeidsgiver ->
            arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                val vedtaksperiodeId = UUID.fromString(vedtaksperiode["id"].asText())
                val behovId = session.behovForVedtaksperide(vedtaksperiodeId)
                vedtaksperiode as ObjectNode
                vedtaksperiode.put("oppgavereferanse", behovId.toString())
            }
        }

        PersonForSpeilDto(
            aktørId = speilSnapshot.aktørId,
            fødselsnummer = speilSnapshot.fødselsnummer,
            navn = NavnDto(vedtak.fornavn, vedtak.mellomnavn, vedtak.etternavn),
            arbeidsgivere = arbeidsgivere,
            infotrygdutbetalinger = infotrygdutbetalinger
        )
    }
}
