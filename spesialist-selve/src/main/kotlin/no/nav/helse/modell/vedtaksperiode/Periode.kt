package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate

internal class Periode(
    private val fom: LocalDate,
    private val tom: LocalDate,
) {
    internal fun fom() = fom
    internal fun tom() = tom
    override fun equals(other: Any?): Boolean =
        this === other || (other is Periode
            && javaClass == other.javaClass
            && fom == other.fom
            && tom == other.tom
        )

    override fun hashCode(): Int {
        var result = fom.hashCode()
        result = 31 * result + tom.hashCode()
        return result
    }

    override fun toString(): String {
        return "Periode(fom=$fom, tom=$tom)"
    }
}