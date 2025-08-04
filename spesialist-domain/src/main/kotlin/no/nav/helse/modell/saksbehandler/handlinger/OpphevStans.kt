package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler

class OpphevStans(
    override val fødselsnummer: String,
    val begrunnelse: String,
) : Personhandling {
    override fun loggnavn(): String = "opphev_stans"

    override fun utførAv(legacySaksbehandler: LegacySaksbehandler) {
        legacySaksbehandler.håndter(this)
    }
}
