import java.util.UUID
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

object TilgangskontrollForTestHarIkkeTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: List<Egenskap>): Boolean {
        return false
    }
}
object TilgangskontrollForTestHarTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: List<Egenskap>): Boolean {
        return true
    }
}

object TilgangskontrollForTestMedKunRiskQA: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: List<Egenskap>): Boolean {
        return egenskaper == listOf(RISK_QA)
    }
}