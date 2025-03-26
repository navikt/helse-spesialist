package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilPerioder
import java.time.LocalDate
import java.time.LocalDateTime

data class Fordelingsdifferanse(
    val nyeEllerEndredeInntekter: List<Inntektsendringer.PeriodeMedBeløp>,
    val fjernedeInntekter: List<Periode>,
)

class Inntektsfordeling private constructor(
    val periodebeløp: Double,
    val opprettet: LocalDateTime,
    val dager: List<LocalDate>,
) {
    private val dagbeløp get() = periodebeløp / dager.size

    internal infix fun diff(forrigeFordeling: Inntektsfordeling): Fordelingsdifferanse {
        val forrigeDagerMap = forrigeFordeling.dager.associateBy { it }
        val nyeDagerMap = this.dager.associateBy { it }

        val lagtTilEllerEndret = mutableListOf<LocalDate>()
        val fjernet = mutableListOf<LocalDate>()

        forrigeDagerMap.forEach { (dato, dag) ->
            val nyDag = nyeDagerMap[dato]
            if (nyDag == null) {
                fjernet.add(dag)
            } else if (dagbeløp != forrigeFordeling.dagbeløp) {
                lagtTilEllerEndret.add(dag)
            }
            // else, dagen har fått fordelt inntekt fra før av og dagbeløpet er likt
        }

        nyeDagerMap.forEach { (dato, dag) ->
            if (forrigeDagerMap[dato] == null) {
                lagtTilEllerEndret.add(dag)
            }
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

    companion object {
        fun ny(
            periodebeløp: Double,
            dager: List<LocalDate>,
        ) = Inntektsfordeling(
            opprettet = LocalDateTime.now(),
            dager = dager,
            periodebeløp = periodebeløp,
        )
    }
}
