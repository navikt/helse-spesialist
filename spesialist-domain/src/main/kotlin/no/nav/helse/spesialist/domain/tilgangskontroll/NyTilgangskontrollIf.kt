package no.nav.helse.spesialist.domain.tilgangskontroll

import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.domain.Saksbehandler

interface NyTilgangskontrollIf {
    fun harTilgangTilPerson(
        saksbehandlerTilganger: SaksbehandlerTilganger,
        fødselsnummer: String,
    ): Boolean

    fun harTilgangTilOppgaveMedEgenskaper(
        egenskaper: Set<Egenskap>,
        saksbehandler: Saksbehandler,
    ): Boolean

    fun harTilgangTilOppgaveMedEgenskaper(
        egenskaper: Set<Egenskap>,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean

    fun harTilgangTilOppgaveMedEgenskap(
        egenskap: Egenskap,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean
}
