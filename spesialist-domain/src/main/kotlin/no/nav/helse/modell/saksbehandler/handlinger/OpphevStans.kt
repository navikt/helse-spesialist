package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper

class OpphevStans(
    override val fødselsnummer: String,
    val begrunnelse: String,
) : Personhandling {
    override fun loggnavn(): String = "opphev_stans"

    override fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper) {
        saksbehandlerWrapper.håndter(this)
    }
}
