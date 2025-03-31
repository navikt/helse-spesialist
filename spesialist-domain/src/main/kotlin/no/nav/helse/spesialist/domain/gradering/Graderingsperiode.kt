package no.nav.helse.spesialist.domain.gradering

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

data class Graderingsdifferanse(
    val nyeEllerEndredeInntekter: List<Inntektsendringer.PeriodeMedBeløp>,
    val fjernedeInntekter: List<Periode>,
)

data class Graderingsperiode(
    val periode: Periode,
    val opprettet: LocalDateTime,
    val periodebeløp: BigDecimal,
    val graderteDager: List<LocalDate>,
) {
    private val dagbeløp get() = periodebeløp.divide(graderteDager.size.toBigDecimal(), RoundingMode.HALF_UP)

    internal infix fun differanseFra(forrigeGraderingsperiode: Graderingsperiode): Graderingsdifferanse {
        val lagtTilEllerEndret = mutableListOf<LocalDate>()
        val fjernet = mutableListOf<LocalDate>()

        if (graderteDager.isEmpty()) {
            return Graderingsdifferanse(
                nyeEllerEndredeInntekter = emptyList(),
                fjernedeInntekter = forrigeGraderingsperiode.graderteDager.tilPerioder(),
            )
        }

        forrigeGraderingsperiode.graderteDager.forEach { dato ->
            when {
                dato !in graderteDager -> fjernet.add(dato)
                dagbeløp != forrigeGraderingsperiode.dagbeløp -> lagtTilEllerEndret.add(dato)
            }
        }

        graderteDager.forEach { dato ->
            if (dato !in forrigeGraderingsperiode.graderteDager) lagtTilEllerEndret.add(dato)
        }

        return Graderingsdifferanse(
            nyeEllerEndredeInntekter = lagtTilEllerEndret.sortedBy { it }.tilPerioderMedBeløp(),
            fjernedeInntekter = fjernet.tilPerioder(),
        )
    }

    internal fun dagerTilPerioderMedBeløp() = graderteDager.tilPerioderMedBeløp()

    private fun List<LocalDate>.tilPerioderMedBeløp(): List<Inntektsendringer.PeriodeMedBeløp> {
        val dagbeløp = dagbeløp.toDouble()
        return this.tilPerioder().map {
            Inntektsendringer.PeriodeMedBeløp(it, dagbeløp)
        }
    }

    companion object {
        fun ny(
            fom: LocalDate,
            tom: LocalDate,
            dager: List<LocalDate>,
            periodebeløp: Double,
        ) = Graderingsperiode(
            periode = Periode(fom, tom),
            opprettet = LocalDateTime.now(),
            periodebeløp = periodebeløp.toBigDecimal(),
            graderteDager = dager,
        )

        internal infix fun List<Graderingsperiode>.overlapperMed(graderingsperiode: Graderingsperiode): Boolean =
            any { it.periode overlapperMed graderingsperiode.periode }
    }
}
