package no.nav.helse.mediator

import no.nav.helse.MeldingPubliserer
import no.nav.helse.modell.melding.KlargjørPersonForVisning
import no.nav.helse.modell.melding.OppdaterPersondata
import no.nav.helse.spesialist.api.Personhåndterer

class PersonhåndtererImpl(
    private val publiserer: MeldingPubliserer,
) : Personhåndterer {
    override fun oppdaterSnapshot(fødselsnummer: String) {
        publiserer.publiser(fødselsnummer, OppdaterPersondata, "oppdaterSnapshot")
    }

    override fun klargjørPersonForVisning(fødselsnummer: String) {
        publiserer.publiser(fødselsnummer, KlargjørPersonForVisning, "klargjørPersonForVisning")
    }
}
