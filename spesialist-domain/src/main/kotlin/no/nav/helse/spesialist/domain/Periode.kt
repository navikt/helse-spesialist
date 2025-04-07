package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) : ValueObject {
    init {
        require(fom <= tom) { "Fom kan ikke være etter tom" }
    }

    fun datoer(): List<LocalDate> = fom.datesUntil(tom.plusDays(1)).toList()

    infix fun overlapper(other: Periode) = fom <= other.tom && tom >= other.fom

    infix fun erInnenfor(other: Periode) = fom >= other.fom && tom <= other.tom

    fun etterfølgesAv(dato: LocalDate): Boolean = tom.plusDays(1) == dato

    operator fun ClosedRange<LocalDate>.contains(closedRange: ClosedRange<LocalDate>): Boolean =
        start <= closedRange.start && endInclusive >= closedRange.endInclusive

    companion object {
        infix fun LocalDate.tilOgMed(other: LocalDate) = Periode(this, other)

        fun Collection<LocalDate>.tilPerioder(): List<Periode> =
            sorted().fold(emptyList()) { acc, dato ->
                val forrigePeriode = acc.lastOrNull()

                if (forrigePeriode != null && forrigePeriode.etterfølgesAv(dato)) {
                    acc.dropLast(1) + forrigePeriode.copy(tom = dato)
                } else {
                    acc + (dato tilOgMed dato)
                }
            }
    }
}
