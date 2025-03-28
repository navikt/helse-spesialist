package no.nav.helse.spesialist.domain

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    init {
        require(fom <= tom) { "Fom kan ikke være etter tom" }
    }

    fun datoer(): List<LocalDate> = fom.datesUntil(tom.plusDays(1)).toList()

    infix fun overlapperMed(other: Periode) = this.overlapper(other) || other.overlapper(this)

    fun forlengesAv(dato: LocalDate): Boolean = tom.plusDays(1) == dato

    private fun overlapper(other: Periode) = other.fom in fom..tom || other.tom in fom..tom

    override fun toString(): String = "Periode(fom=$fom, tom=$tom)"

    companion object {
        infix fun LocalDate.til(other: LocalDate) = Periode(this, other)

        internal fun List<LocalDate>.tilPerioder(): List<Periode> =
            fold(emptyList()) { acc, dato ->
                val sistePeriode = acc.lastOrNull()

                if (sistePeriode != null && sistePeriode.forlengesAv(dato)) {
                    acc.dropLast(1) + sistePeriode.copy(tom = dato)
                } else {
                    acc + (dato til dato)
                }
            }
    }
}
