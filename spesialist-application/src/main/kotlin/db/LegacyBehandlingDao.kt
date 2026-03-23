package no.nav.helse.db

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface LegacyBehandlingDao {
    fun førsteLegacyBehandlingVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime?

    fun finnLegacyBehandlinger(vedtaksperiodeId: UUID): List<BehandlingDto>

    fun finnLegacyBehandling(behandlingDto: BehandlingDto)

    fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID>

    fun førsteKjenteDag(fødselsnummer: String): LocalDate?
}
