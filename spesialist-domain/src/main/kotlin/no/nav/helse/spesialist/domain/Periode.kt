package no.nav.helse.spesialist.domain

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    init {
        require(fom <= tom) { "Fom kan ikke være etter tom" }
    }

    fun overlapperMed(other: Periode) = this.overlapper(other) || other.overlapper(this)

    private fun overlapper(other: Periode) = other.fom in fom..tom || other.tom in fom..tom

    override fun toString(): String = "Periode(fom=$fom, tom=$tom)"

    companion object {
        infix fun LocalDate.til(other: LocalDate) = Periode(this, other)
    }
}
