import java.util.UUID
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

object TilgangskontrollForTestHarIkkeTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: Collection<Egenskap>): Boolean {
        return false
    }
}
object TilgangskontrollForTestHarTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: Collection<Egenskap>): Boolean {
        return true
    }
}