package no.nav.helse.db

import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import java.time.LocalDateTime

interface StansAutomatiskBehandlingRepository {
    fun hentFor(fødselsnummer: String): List<StansAutomatiskBehandlingFraDatabase>

    fun lagreFraISyfo(
        fødselsnummer: String,
        status: String,
        årsaker: Set<StoppknappÅrsak>,
        opprettet: LocalDateTime,
        originalMelding: String?,
        kilde: String,
    )

    fun lagreFraSpeil(fødselsnummer: String)
}
