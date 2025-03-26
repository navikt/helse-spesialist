package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.ddd.Entity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class InntektsperiodeId(val value: UUID)

class Inntektsperiode private constructor(
    id: InntektsperiodeId,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val opprettet: LocalDateTime,
    val fom: LocalDate,
    val tom: LocalDate,
    val periodebeløp: Double,
    fordelinger: List<Fordeling>,
) : Entity<InntektsperiodeId>(id) {
    private val _fordelinger: MutableList<Fordeling> = fordelinger.toMutableList()

    val fordelinger: List<Fordeling> get() = _fordelinger

    fun nyFordeling(fordeling: Fordeling): Inntektsendringer {
        _fordelinger.addLast(fordeling)
        val forrigeFordeling = fordelinger.lastOrNull() ?: return inntektsendringerForFørsteFordeling(fordeling)

        return fordeling.diff(organisasjonsnummer, forrigeFordeling)
    }

    private fun inntektsendringerForFørsteFordeling(fordeling: Fordeling): Inntektsendringer {
        val nyeEllerEndredeInntekter = fordeling.dagerTilPerioderMedBeløp()

        return Inntektsendringer(
            organisasjonsnummer = organisasjonsnummer,
            nyeEllerEndredeInntekter = nyeEllerEndredeInntekter,
            fjernedeInntekter = emptyList(),
        )
    }

    companion object {
        fun ny(
            fødselsnummer: String,
            organisasjonsnummer: String,
            fom: LocalDate,
            tom: LocalDate,
            periodebeløp: Double,
        ) = Inntektsperiode(
            id = InntektsperiodeId(UUID.randomUUID()),
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            opprettet = LocalDateTime.now(),
            fom = fom,
            tom = tom,
            periodebeløp = periodebeløp,
            fordelinger = emptyList(),
        )
    }
}

data class Fordeling private constructor(
    val opprettet: LocalDateTime,
    val dager: List<InntektsDag>,
) {
    fun diff(
        organisasjonsnummer: String,
        forrigeFordeling: Fordeling,
    ): Inntektsendringer {
        val forrigeDagerMap = forrigeFordeling.dager.associateBy { it.dato }
        val nyeDagerMap = this.dager.associateBy { it.dato }

        val lagtTilEllerEndret = mutableListOf<InntektsDag>()
        val fjernet = mutableListOf<InntektsDag>()

        forrigeDagerMap.forEach { (dato, dag) ->
            when (nyeDagerMap[dato]) {
                null -> fjernet.add(dag)
                dag -> {}
                else -> lagtTilEllerEndret.add(dag)
            }
        }

        nyeDagerMap.forEach { (dato, dag) ->
            if (dato !in forrigeDagerMap) {
                lagtTilEllerEndret.add(dag)
            }
        }
        return Inntektsendringer(organisasjonsnummer, lagtTilEllerEndret.tilPerioderMedBeløp(), emptyList())
    }

    private fun List<InntektsDag>.tilPerioderMedBeløp(): List<Inntektsendringer.PeriodeMedBeløp> {
        return fold(emptyList<Inntektsendringer.PeriodeMedBeløp>()) { acc, dag ->
            val sistePeriodeMedBeløp = acc.lastOrNull()

            if (sistePeriodeMedBeløp != null && sistePeriodeMedBeløp.hengerSammenMed(dag)) {
                acc.dropLast(1) + sistePeriodeMedBeløp.copy(tom = dag.dato)
            } else {
                acc + Inntektsendringer.PeriodeMedBeløp(dag.dato, dag.dato, dag.beløp)
            }
        }
    }

    fun dagerTilPerioderMedBeløp() = dager.tilPerioderMedBeløp()

    companion object {
        fun ny(dager: List<InntektsDag>) =
            Fordeling(
                opprettet = LocalDateTime.now(),
                dager = dager,
            )
    }
}

data class InntektsDag(
    val dato: LocalDate,
    val beløp: Double,
)

data class Inntektsendringer(
    val organisasjonsnummer: String,
    val nyeEllerEndredeInntekter: List<PeriodeMedBeløp>,
    val fjernedeInntekter: List<PeriodeUtenBeløp>,
) {
    data class PeriodeMedBeløp(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagbeløp: Double,
    ) {
        fun hengerSammenMed(dag: InntektsDag) = tom.plusDays(1) == dag.dato && dagbeløp == dag.beløp
    }

    data class PeriodeUtenBeløp(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
