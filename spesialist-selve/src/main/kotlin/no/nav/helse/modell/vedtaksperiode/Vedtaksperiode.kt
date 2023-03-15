package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID

internal class Vedtaksperiode(
    private val vedtaksperiodeId: UUID,
    private val sykefraværstilfelleStartDato: LocalDate,
    private val periode: Periode,
) {

    internal companion object {
        internal fun List<Vedtaksperiode>.oppdaterSykefraværstilfeller(
            generasjonRepository: GenerasjonRepository,
        ) {
            this.forEach {
                it.oppdaterSykefraværstilfelle(generasjonRepository)
            }
        }
    }

    private fun oppdaterSykefraværstilfelle(generasjonRepository: GenerasjonRepository) {
        val åpenGenerasjon = generasjonRepository.finnÅpenGenerasjonFor(vedtaksperiodeId) ?: return
        åpenGenerasjon.oppdaterSykefraværstilfelle(sykefraværstilfelleStartDato, periode, generasjonRepository)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Vedtaksperiode
                && javaClass == other.javaClass
                && vedtaksperiodeId == other.vedtaksperiodeId
                && sykefraværstilfelleStartDato == other.sykefraværstilfelleStartDato
                && periode == other.periode)


    override fun hashCode(): Int {
        var result = vedtaksperiodeId.hashCode()
        result = 31 * result + sykefraværstilfelleStartDato.hashCode()
        result = 31 * result + periode.hashCode()
        return result
    }
}