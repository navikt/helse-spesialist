import java.util.UUID
import no.nav.helse.modell.oppgave.RISK_QA
import no.nav.helse.modell.oppgave.TilgangsstyrtEgenskap
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

object TilgangskontrollForTestHarIkkeTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: List<TilgangsstyrtEgenskap>): Boolean {
        return false
    }
}
object TilgangskontrollForTestHarTilgang: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: List<TilgangsstyrtEgenskap>): Boolean {
        return true
    }
}

object TilgangskontrollForTestMedKunRiskQA: Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: List<TilgangsstyrtEgenskap>): Boolean {
        return egenskaper == listOf(RISK_QA)
    }
}