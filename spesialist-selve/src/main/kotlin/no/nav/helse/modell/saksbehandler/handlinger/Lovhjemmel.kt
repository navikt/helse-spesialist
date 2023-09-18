package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.LovhjemmelEvent
import no.nav.helse.modell.saksbehandler.handlinger.dto.LovhjemmelDto

data class Lovhjemmel(
    private val paragraf: String,
    private val ledd: String? = null,
    private val bokstav: String? = null,
    private val lovverk: String,
    private val lovverksversjon: String,
) {
    fun byggEvent() =
        LovhjemmelEvent(
            paragraf = paragraf,
            ledd = ledd,
            bokstav = bokstav,
            lovverk = lovverk,
            lovverksversjon = lovverksversjon
        )

    fun toDto() = LovhjemmelDto(
        paragraf = paragraf,
        ledd = ledd,
        bokstav = bokstav,
        lovverk = lovverk,
        lovverksversjon = lovverksversjon
    )
}