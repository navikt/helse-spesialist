package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.Daos
import no.nav.helse.mediator.TilgangskontrollørForReservasjon
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler

class NyTilgangskontroll(
    private val daos: Daos,
    private val tilgangsgruppehenter: Tilgangsgruppehenter,
    private val tilgangsgrupper: Tilgangsgrupper,
) {
    fun harTilgangTilPerson(
        saksbehandlerTilganger: SaksbehandlerTilganger,
        fødselsnummer: String,
    ): Boolean = !manglerTilgang(daos.egenAnsattApiDao, daos.personApiDao, fødselsnummer, saksbehandlerTilganger)

    fun harTilgangTilOppgave(
        saksbehandler: LegacySaksbehandler,
        egenskaper: List<Egenskap>,
    ): Boolean = saksbehandler.harTilgangTil(egenskaper)

    fun harTilgangTilOppgave(
        saksbehandler: Saksbehandler,
        egenskaper: List<Egenskap>,
    ): Boolean {
        val tilgangskontroll =
            TilgangskontrollørForReservasjon(
                tilgangsgruppehenter = tilgangsgruppehenter,
                tilgangsgrupper = tilgangsgrupper,
            )
        return tilgangskontroll.harTilgangTil(saksbehandler.id().value, egenskaper)
    }
}
