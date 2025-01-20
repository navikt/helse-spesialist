package no.nav.helse.mediator

import no.nav.helse.MeldingPubliserer
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.saksbehandler.SaksbehandlerObserver

class Subsumsjonsmelder(private val versjonAvKode: String, private val meldingPubliserer: MeldingPubliserer) :
    SaksbehandlerObserver {
    override fun nySubsumsjon(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
    ) {
        meldingPubliserer.publiser(
            fødselsnummer = fødselsnummer,
            subsumsjonEvent = subsumsjonEvent,
            versjonAvKode = versjonAvKode,
        )
    }
}
