package no.nav.helse.db

import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding

interface StansAutomatiskBehandlingDao {
    fun hentFor(fødselsnummer: String): List<StansAutomatiskBehandlingFraDatabase>

    fun lagreFraISyfo(melding: StansAutomatiskBehandlingMelding)

    fun lagreFraSpeil(fødselsnummer: String)
}
