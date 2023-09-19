package no.nav.helse.mediator

import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.modell.oppgave.EGEN_ANSATT
import no.nav.helse.modell.oppgave.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.RISK_QA
import no.nav.helse.modell.oppgave.TilgangsstyrtEgenskap
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

internal interface Gruppekontroll {
    suspend fun erIGruppe(oid: UUID, groupId: UUID): Boolean
}

internal class Tilgangskontrollør(
    private val gruppekontroll: Gruppekontroll,
    private val tilgangsgrupper: Tilgangsgrupper,
): Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskap: TilgangsstyrtEgenskap): Boolean {
        return runBlocking {
            when (egenskap) {
                EGEN_ANSATT -> gruppekontroll.erIGruppe(oid, tilgangsgrupper.skjermedePersonerGruppeId)
                FORTROLIG_ADRESSE -> gruppekontroll.erIGruppe(oid, tilgangsgrupper.kode7GruppeId)
                RISK_QA -> gruppekontroll.erIGruppe(oid, tilgangsgrupper.riskQaGruppeId)
            }
        }
    }
}