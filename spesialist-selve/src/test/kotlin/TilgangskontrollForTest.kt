import java.util.UUID
import no.nav.helse.modell.oppgave.TilgangsstyrtEgenskap
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

object TilgangskontrollForTestHarIkkeTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskap: TilgangsstyrtEgenskap): Boolean {
        return false
    }
}
object TilgangskontrollForTestHarTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskap: TilgangsstyrtEgenskap): Boolean {
        return true
    }
}