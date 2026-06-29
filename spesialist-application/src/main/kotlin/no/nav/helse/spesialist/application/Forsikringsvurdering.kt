package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer

data class Forsikringsvurdering(
    val identitetsnummer: Identitetsnummer,
    val harForsikring: Boolean,
    val dekning: Dekning?,
) {
    data class Dekning(
        val grad: Int,
        val fraDag: Int,
    )
}
