package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDate

data class Infotrygdperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: String,
) : ValueObject
