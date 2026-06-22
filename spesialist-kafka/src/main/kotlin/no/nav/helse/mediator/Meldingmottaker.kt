package no.nav.helse.mediator

import no.nav.helse.MeldingPubliserer
import no.nav.helse.mediator.meldinger.Personmelding

interface Meldingmottaker {
    fun mottaMelding(
        melding: Personmelding,
        kontekstbasertPubliserer: MeldingPubliserer,
    )
}
