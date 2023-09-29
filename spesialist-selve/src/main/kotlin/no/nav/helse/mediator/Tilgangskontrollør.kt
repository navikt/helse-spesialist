package no.nav.helse.mediator

import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Gruppekontroll
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.RISK_QA
import no.nav.helse.modell.oppgave.Egenskap.SPESIALSAK
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

internal abstract class MicrosoftTilgangskontroll(private val tilgangsgrupper: Tilgangsgrupper): Tilgangskontroll {
    final override fun harTilgangTil(oid: UUID, egenskaper: Collection<Egenskap>): Boolean =
        harTilgangTil(oid, egenskaper.tilTilgangsgrupper())

    abstract fun harTilgangTil(oid: UUID, grupper: List<UUID>): Boolean

    private fun Collection<Egenskap>.tilTilgangsgrupper() = map { egenskap ->
        when (egenskap) {
            EGEN_ANSATT -> tilgangsgrupper.skjermedePersonerGruppeId
            FORTROLIG_ADRESSE -> tilgangsgrupper.kode7GruppeId
            RISK_QA -> tilgangsgrupper.riskQaGruppeId
            BESLUTTER -> tilgangsgrupper.beslutterGruppeId
            STIKKPRØVE -> tilgangsgrupper.stikkprøveGruppeId
            SPESIALSAK -> tilgangsgrupper.spesialsakGruppeId
            else -> throw IllegalArgumentException("Egenskap $egenskap er ikke støttet som tilgangsstyrt egenskap")
        }
    }
}

internal class TilgangskontrollørForReservasjon(
    private val gruppekontroll: Gruppekontroll,
    tilgangsgrupper: Tilgangsgrupper,
) : MicrosoftTilgangskontroll(tilgangsgrupper) {

    override fun harTilgangTil(oid: UUID, grupper: List<UUID>): Boolean = runBlocking {
        gruppekontroll.erIGrupper(oid, grupper)
    }
}

internal class TilgangskontrollørForApi(
    private val saksbehandlergrupper: List<UUID>,
    tilgangsgrupper: Tilgangsgrupper,
) : MicrosoftTilgangskontroll(tilgangsgrupper) {
    override fun harTilgangTil(oid: UUID, grupper: List<UUID>): Boolean = saksbehandlergrupper.containsAll(grupper)
}