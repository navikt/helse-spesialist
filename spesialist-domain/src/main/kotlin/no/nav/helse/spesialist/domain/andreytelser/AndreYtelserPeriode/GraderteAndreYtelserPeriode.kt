package no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode

import no.nav.helse.spesialist.domain.Periode

data class GraderteAndreYtelserPeriode(
    val periode: Periode,
    val grad: Int,
)
