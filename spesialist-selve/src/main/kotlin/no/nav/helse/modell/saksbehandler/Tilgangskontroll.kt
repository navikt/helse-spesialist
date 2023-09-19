package no.nav.helse.modell.saksbehandler

import java.util.UUID
import no.nav.helse.modell.oppgave.TilgangsstyrtEgenskap

interface Tilgangskontroll {
    fun harTilgangTil(oid: UUID, egenskap: TilgangsstyrtEgenskap): Boolean
}