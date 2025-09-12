package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.domain.tilgangskontroll.SaksbehandlerTilganger

class NyTilgangskontroll(
    private val egenAnsattApiDao: EgenAnsattApiDao,
    private val personApiDao: PersonApiDao,
) {
    fun harTilgangTilPerson(
        saksbehandlerTilganger: SaksbehandlerTilganger,
        fødselsnummer: String,
    ): Boolean = !manglerTilgang(egenAnsattApiDao, personApiDao, fødselsnummer, saksbehandlerTilganger)
}
