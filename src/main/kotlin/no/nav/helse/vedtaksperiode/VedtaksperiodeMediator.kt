package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dto.ArbeidsgiverForSpeilDto
import no.nav.helse.modell.dto.NavnDto
import no.nav.helse.modell.dto.PersonForSpeilDto
import no.nav.helse.modell.dto.PersonFraSpleisDto
import no.nav.helse.objectMapper
import java.util.*

internal class VedtaksperiodeMediator(
    private val vedtaksperiodeDao: VedtaksperiodeDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val snapshotDao: SnapshotDao
) {
    fun byggSpeilSnapshotForFnr(fnr: String) = vedtaksperiodeDao.findVedtakByFnr(fnr)?.let(::byggSpeilSnapshot)
    fun byggSpeilSnapshotForAktørId(aktørId: String) = vedtaksperiodeDao.findVedtakByAktørId(aktørId)?.let(::byggSpeilSnapshot)
    fun byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeId: UUID) = vedtaksperiodeDao.findVedtakByVedtaksperiodeId(vedtaksperiodeId)?.let(::byggSpeilSnapshot)

    fun byggSpeilSnapshot(vedtak: VedtaksperiodeDto): PersonForSpeilDto {
        val arbeidsgiverDto = requireNotNull(arbeidsgiverDao.findArbeidsgiver(vedtak.arbeidsgiverRef)) { "Fant ikke arbeidsgiver" }
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
            arbeidsgivere
        )
    }
}
