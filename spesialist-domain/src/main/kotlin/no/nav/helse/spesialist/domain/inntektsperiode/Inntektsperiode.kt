package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.ddd.Entity
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class InntektsperiodeId(val value: UUID)

class Inntektsperiode private constructor(
    id: InntektsperiodeId,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val opprettet: LocalDateTime,
    endringer: List<Inntektsperiodeendring>,
) : Entity<InntektsperiodeId>(id) {
    private val _endringer: MutableList<Inntektsperiodeendring> = endringer.toMutableList()

    val endringer: List<Inntektsperiodeendring> get() = _endringer

    fun endring(inntektsperiodeendring: Inntektsperiodeendring): Inntektsendringer {
        val forrigeFordeling = endringer.lastOrNull()?.fordeling
        val nyFordeling = inntektsperiodeendring.fordeling
        _endringer.addLast(inntektsperiodeendring)
        if (forrigeFordeling == null) return inntektsendringerForFørsteFordeling(nyFordeling)

        val differanse = nyFordeling diff forrigeFordeling
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
        ) = Inntektsperiode(
            id = InntektsperiodeId(UUID.randomUUID()),
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            opprettet = LocalDateTime.now(),
            endringer = emptyList(),
        )
    }
}
