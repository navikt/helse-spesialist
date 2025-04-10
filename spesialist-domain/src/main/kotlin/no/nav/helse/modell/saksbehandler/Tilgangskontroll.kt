package no.nav.helse.modell.saksbehandler

import no.nav.helse.modell.oppgave.Egenskap
import java.util.UUID

fun interface Tilgangskontroll {
    fun harTilgangTil(
        oid: UUID,
        egenskaper: Collection<Egenskap>,
    ): Boolean
}
