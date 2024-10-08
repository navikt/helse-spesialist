package no.nav.helse.spesialist.api

interface Personhåndterer {
    fun oppdaterSnapshot(
        fnr: String,
        skalKlargjøresForVisning: Boolean = false,
    )
}
