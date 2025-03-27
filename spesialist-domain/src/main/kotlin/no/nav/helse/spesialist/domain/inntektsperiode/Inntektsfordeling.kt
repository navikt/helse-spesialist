package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import java.time.LocalDate

data class Fordelingsdifferanse(
    val nyeEllerEndredeInntekter: List<Inntektsendringer.PeriodeMedBeløp>,
    val fjernedeInntekter: List<Periode>,
)

data class Inntektsfordeling(
    val periodebeløp: Double,
    val dager: List<LocalDate>,
) {
    private val dagbeløp get() = periodebeløp / dager.size

    internal infix fun diff(forrigeFordeling: Inntektsfordeling): Fordelingsdifferanse {
        val lagtTilEllerEndret = mutableListOf<LocalDate>()
        val fjernet = mutableListOf<LocalDate>()

        forrigeFordeling.dager.forEach { dato ->
            when {
                dato !in dager -> fjernet.add(dato)
                dagbeløp != forrigeFordeling.dagbeløp -> lagtTilEllerEndret.add(dato)
            }
        }

        dager.forEach { dato ->
            if (dato !in forrigeFordeling.dager) lagtTilEllerEndret.add(dato)
        }

        return Fordelingsdifferanse(
            nyeEllerEndredeInntekter = lagtTilEllerEndret.sortedBy { it }.tilPerioderMedBeløp(),
            fjernedeInntekter = fjernet.tilPerioder(),
        )
    }

    internal fun dagerTilPerioderMedBeløp() = dager.tilPerioderMedBeløp()

    private fun List<LocalDate>.tilPerioderMedBeløp(): List<Inntektsendringer.PeriodeMedBeløp> {
        val dagbeløp = dagbeløp
        return this.tilPerioder().map {
            Inntektsendringer.PeriodeMedBeløp(it, dagbeløp)
        }
    }
}
