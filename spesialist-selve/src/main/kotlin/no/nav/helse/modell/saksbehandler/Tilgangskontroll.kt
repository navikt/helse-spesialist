package no.nav.helse.modell.saksbehandler

import java.util.UUID
import no.nav.helse.modell.oppgave.Egenskap

interface Tilgangskontroll {
    fun harTilgangTil(oid: UUID, egenskaper: List<Egenskap>): Boolean
}