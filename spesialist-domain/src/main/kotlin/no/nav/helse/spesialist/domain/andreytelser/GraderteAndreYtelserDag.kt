package no.nav.helse.spesialist.domain.andreytelser

import java.time.LocalDate

typealias Grad = Int

data class GraderteAndreYtelserDag(
    val dato: LocalDate,
    val andreYtelserGrader: Map<GraderteAndreYtelserType, Grad>,
    val sykdomsgrad: Grad,
    val arbeidsgrad: Grad,
)
