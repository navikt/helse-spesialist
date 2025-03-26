package no.nav.helse.spesialist.domain.inntektsperiode

import java.time.LocalDate
import java.time.LocalDateTime

data class Fordelingsdifferanse(
    val nyeEllerEndredeInntekter: List<Inntektsendringer.PeriodeMedBeløp>,
    val fjernedeInntekter: List<Inntektsendringer.PeriodeUtenBeløp>,
)

class Inntektsfordeling private constructor(
    val opprettet: LocalDateTime,
    val dager: List<Inntektsdag>,
) {
    internal infix fun diff(forrigeFordeling: Inntektsfordeling): Fordelingsdifferanse {
        val forrigeDagerMap = forrigeFordeling.dager.associateBy { it.dato }
        val nyeDagerMap = this.dager.associateBy { it.dato }

        val lagtTilEllerEndret = mutableListOf<Inntektsdag>()
        val fjernet = mutableListOf<Inntektsdag>()

        forrigeDagerMap.forEach { (dato, dag) ->
            when (val nyDag = nyeDagerMap[dato]) {
                null -> fjernet.add(dag)
                dag -> {}
                else -> lagtTilEllerEndret.add(nyDag)
            }
        }

        nyeDagerMap.forEach { (dato, dag) ->
            if (dato !in forrigeDagerMap) {
                lagtTilEllerEndret.add(dag)
            }
        }
        return Fordelingsdifferanse(
            nyeEllerEndredeInntekter = lagtTilEllerEndret.tilPerioderMedBeløp(),
            fjernedeInntekter = fjernet.tilPerioderUtenBeløp(),
        )
    }

    internal fun dagerTilPerioderMedBeløp() = dager.tilPerioderMedBeløp()

    private fun List<Inntektsdag>.tilPerioderMedBeløp(): List<Inntektsendringer.PeriodeMedBeløp> {
        return fold(emptyList()) { acc, dag ->
            val sistePeriodeMedBeløp = acc.lastOrNull()

            if (sistePeriodeMedBeløp != null && sistePeriodeMedBeløp.hengerSammenMed(dag)) {
                acc.dropLast(1) + sistePeriodeMedBeløp.copy(tom = dag.dato)
            } else {
                acc + Inntektsendringer.PeriodeMedBeløp(dag.dato, dag.dato, dag.beløp)
            }
        }
    }

    private fun List<Inntektsdag>.tilPerioderUtenBeløp(): List<Inntektsendringer.PeriodeUtenBeløp> {
        return fold(emptyList()) { acc, dag ->
            val sistePeriodeUtenBeløp = acc.lastOrNull()

            if (sistePeriodeUtenBeløp != null && sistePeriodeUtenBeløp.hengerSammenMed(dag)) {
                acc.dropLast(1) + sistePeriodeUtenBeløp.copy(tom = dag.dato)
            } else {
                acc + Inntektsendringer.PeriodeUtenBeløp(dag.dato, dag.dato)
            }
        }
    }

    companion object {
        fun ny(dager: List<Inntektsdag>) =
            Inntektsfordeling(
                opprettet = LocalDateTime.now(),
                dager = dager,
            )
    }
}

data class Inntektsdag(
    val dato: LocalDate,
    val beløp: Double,
)
