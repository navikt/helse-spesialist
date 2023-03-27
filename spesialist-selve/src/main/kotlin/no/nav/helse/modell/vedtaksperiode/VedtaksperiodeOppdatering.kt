package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeOppdatering(
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val vedtaksperiodeId: UUID
) {
    private fun oppdaterSykefraværstilfelle(generasjonRepository: GenerasjonRepository) {
        val åpenGenerasjon = generasjonRepository.finnÅpenGenerasjonFor(vedtaksperiodeId) ?: return
        åpenGenerasjon.oppdaterSykefraværstilfelle(skjæringstidspunkt, Periode(fom, tom), generasjonRepository)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is VedtaksperiodeOppdatering
                && javaClass == other.javaClass
                && vedtaksperiodeId == other.vedtaksperiodeId
                && skjæringstidspunkt == other.skjæringstidspunkt
                && fom == other.fom
                && tom == other.tom)

    override fun hashCode(): Int {
        var result = vedtaksperiodeId.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + fom.hashCode()
        result = 31 * result + tom.hashCode()
        return result
    }

    internal companion object {
        internal fun List<VedtaksperiodeOppdatering>.oppdaterSykefraværstilfeller(
            generasjonRepository: GenerasjonRepository,
        ) {
            this.forEach {
                it.oppdaterSykefraværstilfelle(generasjonRepository)
            }
        }
    }
}

