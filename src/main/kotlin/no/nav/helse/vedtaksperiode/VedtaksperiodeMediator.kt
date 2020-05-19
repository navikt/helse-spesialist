package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.modell.vedtak.NavnDto
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import no.nav.helse.objectMapper
import java.util.*

internal class VedtaksperiodeMediator(
    private val vedtaksperiodeDao: VedtaksperiodeDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val snapshotDao: SnapshotDao,
    private val personDao: PersonDao
) {
    fun byggSpeilSnapshotForFnr(fnr: String) = vedtaksperiodeDao.findVedtakByFnr(fnr)?.let(::byggSpeilSnapshot)
    fun byggSpeilSnapshotForAktørId(aktørId: String) = vedtaksperiodeDao.findVedtakByAktørId(aktørId)?.let(::byggSpeilSnapshot)
    fun byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeId: UUID) = vedtaksperiodeDao.findVedtakByVedtaksperiodeId(vedtaksperiodeId)?.let(::byggSpeilSnapshot)

    private fun byggSpeilSnapshot(vedtak: VedtaksperiodeDto): PersonForSpeilDto {
        val arbeidsgiverDto = requireNotNull(arbeidsgiverDao.findArbeidsgiver(vedtak.arbeidsgiverRef)) { "Fant ikke arbeidsgiver" }
        val infotrygdutbetalinger = vedtak.infotrygdutbetalingerRef
            ?.let { personDao.findInfotrygdutbetalinger(it) }
            ?.let { objectMapper.readTree(it) }
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
        return PersonForSpeilDto(
            speilSnapshot.aktørId,
            speilSnapshot.fødselsnummer,
            NavnDto(vedtak.fornavn, vedtak.mellomnavn, vedtak.etternavn),
            arbeidsgivere,
            infotrygdutbetalinger
        )
    }
}
