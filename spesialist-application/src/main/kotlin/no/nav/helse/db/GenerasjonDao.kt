package no.nav.helse.db

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface GenerasjonDao {
    fun førsteGenerasjonVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime?

    fun finnGenerasjoner(vedtaksperiodeId: UUID): List<BehandlingDto>

    fun lagreGenerasjon(behandlingDto: BehandlingDto)

    fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID>

    fun førsteKjenteDag(fødselsnummer: String): LocalDate
}
