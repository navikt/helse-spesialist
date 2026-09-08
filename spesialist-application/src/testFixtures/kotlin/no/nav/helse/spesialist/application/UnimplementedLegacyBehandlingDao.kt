package no.nav.helse.spesialist.application

import no.nav.helse.db.LegacyBehandlingDao
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class UnimplementedLegacyBehandlingDao : LegacyBehandlingDao {
    private val førsteVedtakFattetTidspunkt = mutableMapOf<UUID, LocalDateTime>()

    fun settFørsteLegacyBehandlingVedtakFattetTidspunkt(
        vedtaksperiodeId: UUID,
        tidspunkt: LocalDateTime?,
    ) {
        if (tidspunkt == null) {
            førsteVedtakFattetTidspunkt.remove(vedtaksperiodeId)
        } else {
            førsteVedtakFattetTidspunkt[vedtaksperiodeId] = tidspunkt
        }
    }

    override fun førsteLegacyBehandlingVedtakFattetTidspunkt(vedtaksperiodeId: UUID): LocalDateTime? = førsteVedtakFattetTidspunkt[vedtaksperiodeId]

    override fun finnLegacyBehandlinger(vedtaksperiodeId: UUID): List<BehandlingDto> {
        TODO("Not yet implemented")
    }

    override fun finnVedtaksperiodeIderFor(fødselsnummer: String): Set<UUID> {
        TODO("Not yet implemented")
    }

    override fun førsteKjenteDag(fødselsnummer: String): LocalDate? {
        TODO("Not yet implemented")
    }
}
