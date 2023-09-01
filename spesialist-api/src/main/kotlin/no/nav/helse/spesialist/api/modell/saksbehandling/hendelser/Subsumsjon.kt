package no.nav.helse.spesialist.api.modell.saksbehandling.hendelser

import no.nav.helse.spesialist.api.modell.SubsumsjonEvent

internal class Subsumsjon(
    private val paragraf: String,
    private val ledd: String? = null,
    private val bokstav: String? = null,
) {
    internal fun byggEvent() =
        SubsumsjonEvent(paragraf, ledd, bokstav)
}