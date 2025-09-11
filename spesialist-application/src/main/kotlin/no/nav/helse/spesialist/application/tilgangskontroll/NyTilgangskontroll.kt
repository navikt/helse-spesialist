package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.Daos
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler

class NyTilgangskontroll(
    private val daos: Daos,
) {
    fun harTilgangTilPerson(
        saksbehandlerTilganger: SaksbehandlerTilganger,
        fødselsnummer: String,
    ): Boolean = !manglerTilgang(daos.egenAnsattApiDao, daos.personApiDao, fødselsnummer, saksbehandlerTilganger)

    fun harTilgangTilOppgave(
        saksbehandler: LegacySaksbehandler,
        egenskaper: List<Egenskap>,
    ): Boolean = saksbehandler.harTilgangTil(egenskaper)
}
