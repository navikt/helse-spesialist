package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID

internal class Vedtaksperiode(
    private val vedtaksperiodeId: UUID,
    private val gjeldendeGenerasjon: Generasjon
) {

    private val observers = mutableListOf<IVedtaksperiodeObserver>()

    internal fun registrer(observer: IVedtaksperiodeObserver) {
        observers.add(observer)
    }

    internal fun håndterTidslinjeEndring(fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
        observers.forEach{ it.tidslinjeOppdatert(vedtaksperiodeId, fom, tom, skjæringstidspunkt)}
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Vedtaksperiode
                && javaClass == other.javaClass
                && vedtaksperiodeId == other.vedtaksperiodeId
                && gjeldendeGenerasjon == other.gjeldendeGenerasjon
                )

    override fun hashCode(): Int {
        var result = vedtaksperiodeId.hashCode()
        result = 31 * result + gjeldendeGenerasjon.hashCode()
        return result
    }

}