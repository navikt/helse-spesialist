package no.nav.helse.mediator

import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SPESIALSAK
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

internal interface Gruppekontroll {
    suspend fun erIGrupper(oid: UUID, gruppeIder: List<UUID>): Boolean
}

internal class Tilgangskontrollør(
    private val gruppekontroll: Gruppekontroll,
    private val tilgangsgrupper: Tilgangsgrupper,
) : Tilgangskontroll {
    override fun harTilgangTil(oid: UUID, egenskaper: List<Egenskap>): Boolean {
        return runBlocking {
            gruppekontroll.erIGrupper(oid, egenskaper.map { mapTilgangsgruppe(it) })
        }
    }

    private fun mapTilgangsgruppe(egenskap: Egenskap) = when (egenskap) {
        EGEN_ANSATT -> tilgangsgrupper.skjermedePersonerGruppeId
        FORTROLIG_ADRESSE -> tilgangsgrupper.kode7GruppeId
        RISK_QA -> tilgangsgrupper.riskQaGruppeId
        BESLUTTER -> tilgangsgrupper.beslutterGruppeId
        STIKKPRØVE -> tilgangsgrupper.stikkprøveGruppeId
        SPESIALSAK -> tilgangsgrupper.spesialsakGruppeId
        else -> throw IllegalArgumentException("Egenskap $egenskap er ikke støttet som tilgangsstyrt egenskap")
    }
}