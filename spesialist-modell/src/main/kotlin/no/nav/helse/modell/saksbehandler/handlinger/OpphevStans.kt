package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler

class OpphevStans(override val fødselsnummer: String, val begrunnelse: String) : Personhandling {
    override fun loggnavn(): String = "opphev_stans"

    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }
}
