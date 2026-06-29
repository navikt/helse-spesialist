package no.nav.helse.spesialist.domain

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

sealed interface ResultatAvForsikring {
    data class MottattForsikring(
        val forsikring: Forsikring,
    ) : ResultatAvForsikring

    data object IngenForsikring : ResultatAvForsikring
}

class Forsikring(
    val gjelderFraDag: Int,
    val dekningsgrad: Int,
) {
    object Factory {
        fun ny(
            gjelderFraDag: Int,
            dekningsgrad: Int,
        ) = Forsikring(gjelderFraDag, dekningsgrad)
    }
}
