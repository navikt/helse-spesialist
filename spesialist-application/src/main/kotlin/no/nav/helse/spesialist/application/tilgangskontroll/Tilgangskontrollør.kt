package no.nav.helse.spesialist.application.tilgangskontroll

import kotlinx.coroutines.runBlocking
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import java.util.UUID

abstract class MicrosoftTilgangskontroll(
    private val tilgangsgrupper: Tilgangsgrupper,
) : Tilgangskontroll {
    final override fun harTilgangTil(
        oid: UUID,
        egenskaper: Collection<Egenskap>,
    ): Boolean = egenskaper.isEmpty() || harTilgangTil(oid, egenskaper.tilTilgangsgrupper())

    protected abstract fun harTilgangTil(
        oid: UUID,
        grupper: List<UUID>,
    ): Boolean

    private fun Collection<Egenskap>.tilTilgangsgrupper() =
        map { egenskap ->
            when (egenskap) {
                Egenskap.EGEN_ANSATT -> tilgangsgrupper.skjermedePersonerGruppeId
                Egenskap.FORTROLIG_ADRESSE -> tilgangsgrupper.kode7GruppeId
                Egenskap.STRENGT_FORTROLIG_ADRESSE -> UUID.randomUUID() // Ingen skal ha tilgang til disse i Speil foreløpig
                Egenskap.BESLUTTER -> tilgangsgrupper.beslutterGruppeId
                Egenskap.STIKKPRØVE -> tilgangsgrupper.stikkprøveGruppeId
                else -> throw IllegalArgumentException("Egenskap $egenskap er ikke støttet som tilgangsstyrt egenskap")
            }
        }
}

class TilgangskontrollørForReservasjon(
    private val tilgangsgruppehenter: Tilgangsgruppehenter,
    tilgangsgrupper: Tilgangsgrupper,
) : MicrosoftTilgangskontroll(tilgangsgrupper) {
    override fun harTilgangTil(
        oid: UUID,
        grupper: List<UUID>,
    ): Boolean =
        runBlocking {
            tilgangsgruppehenter.hentTilgangsgrupper(oid, grupper).containsAll(grupper)
        }
}

class TilgangskontrollørForApi(
    private val saksbehandlergrupper: List<UUID>,
    tilgangsgrupper: Tilgangsgrupper,
) : MicrosoftTilgangskontroll(tilgangsgrupper) {
    override fun harTilgangTil(
        oid: UUID,
        grupper: List<UUID>,
    ): Boolean = saksbehandlergrupper.containsAll(grupper)
}
