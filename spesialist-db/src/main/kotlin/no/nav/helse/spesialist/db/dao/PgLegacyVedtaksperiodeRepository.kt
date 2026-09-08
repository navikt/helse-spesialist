package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.LegacyBehandlingDao
import no.nav.helse.db.LegacyVedtaksperiodeRepository
import no.nav.helse.db.VedtakDao
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import java.util.UUID

class PgLegacyVedtaksperiodeRepository(
    private val legacyBehandlingDao: LegacyBehandlingDao,
    private val vedtakDao: VedtakDao,
) : LegacyVedtaksperiodeRepository {
    private val hentedeBehandlinger: MutableMap<UUID, List<BehandlingDto>> = mutableMapOf()

    override fun finnVedtaksperioder(fødselsnummer: String): List<VedtaksperiodeDto> = legacyBehandlingDao.finnVedtaksperiodeIderFor(fødselsnummer).map { finnVedtaksperiode(it) }

    private fun finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto =
        vedtakDao
            .finnVedtaksperiode(vedtaksperiodeId)
            ?.copy(behandlinger = finnBehandlinger(vedtaksperiodeId))
            ?: throw IllegalStateException("Forventer å finne vedtaksperiode for vedtaksperiodeId=$vedtaksperiodeId")

    private fun finnBehandlinger(vedtaksperiodeId: UUID): List<BehandlingDto> =
        legacyBehandlingDao.finnLegacyBehandlinger(vedtaksperiodeId).also {
            hentedeBehandlinger[vedtaksperiodeId] = it
        }

    override fun førsteKjenteDag(fødselsnummer: String) = legacyBehandlingDao.førsteKjenteDag(fødselsnummer)
}
