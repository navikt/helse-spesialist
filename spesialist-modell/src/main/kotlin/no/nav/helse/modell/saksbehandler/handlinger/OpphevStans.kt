package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler

class OpphevStans(private val fødselsnummer: String, private val begrunnelse: String) : Personhandling {
    override fun loggnavn(): String = "opphev_stans"

    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun gjelderFødselsnummer(): String = fødselsnummer

    override fun begrunnelse(): String = begrunnelse
}
