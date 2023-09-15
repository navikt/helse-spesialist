package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.handlinger.dto.LovhjemmelDto
import no.nav.helse.spesialist.api.modell.LovhjemmelEvent

data class Lovhjemmel(
    private val paragraf: String,
    private val ledd: String? = null,
    private val bokstav: String? = null,
) {
    fun byggEvent() =
        LovhjemmelEvent(paragraf, ledd, bokstav)

    fun toDto() = LovhjemmelDto(paragraf = paragraf, ledd = ledd, bokstav = bokstav)
}