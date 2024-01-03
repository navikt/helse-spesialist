package no.nav.helse.modell.saksbehandler.handlinger

import java.util.UUID
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

internal object TilgangskontrollForTestHarIkkeTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: Collection<Egenskap>): Boolean {
        return false
    }
}
internal object TilgangskontrollForTestHarTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: Collection<Egenskap>): Boolean {
        return true
    }
}

internal object TilgangskontrollForTestMedKunFortroligAdresse: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: Collection<Egenskap>): Boolean {
        return egenskaper == listOf(FORTROLIG_ADRESSE)
    }
}