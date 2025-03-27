package no.nav.helse.spesialist.domain.inntektsperiode

import no.nav.helse.spesialist.domain.Periode
import java.time.LocalDate
import java.time.LocalDateTime

class Inntektsperiodeendring private constructor(
    val periode: Periode,
    val opprettet: LocalDateTime,
    val fordeling: Inntektsfordeling,
) {
    companion object {
        fun ny(
            fom: LocalDate,
            tom: LocalDate,
            fordeling: Inntektsfordeling,
        ) = Inntektsperiodeendring(
            periode = Periode(fom, tom),
            opprettet = LocalDateTime.now(),
            fordeling = fordeling,
        )
    }
}
