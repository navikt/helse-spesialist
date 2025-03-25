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
    fordelinger: List<Inntektsfordeling>,
) : Entity<InntektsperiodeId>(id) {
    private val _fordelinger: MutableList<Inntektsfordeling> = fordelinger.toMutableList()

    val fordelinger: List<Inntektsfordeling> get() = _fordelinger

    fun nyFordeling(fordeling: Inntektsfordeling): Inntektsendringer {
        val forrigeFordeling = fordelinger.lastOrNull()
        _fordelinger.addLast(fordeling)
        if (forrigeFordeling == null) return inntektsendringerForFørsteFordeling(fordeling)

        val differanse = fordeling diff forrigeFordeling
        return Inntektsendringer(
            organisasjonsnummer = organisasjonsnummer,
            nyeEllerEndredeInntekter = differanse.nyeEllerEndredeInntekter,
            fjernedeInntekter = differanse.fjernedeInntekter,
        )
    }

    private fun inntektsendringerForFørsteFordeling(fordeling: Inntektsfordeling): Inntektsendringer {
        return Inntektsendringer(
            organisasjonsnummer = organisasjonsnummer,
            nyeEllerEndredeInntekter = fordeling.dagerTilPerioderMedBeløp(),
            fjernedeInntekter = emptyList(),
        )
    }

    companion object {
        fun ny(
            fødselsnummer: String,
            organisasjonsnummer: String,
            fom: LocalDate,
            tom: LocalDate,
        ) = Inntektsperiode(
            id = InntektsperiodeId(UUID.randomUUID()),
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            opprettet = LocalDateTime.now(),
            fom = fom,
            tom = tom,
            fordelinger = emptyList(),
        )
    }
}
