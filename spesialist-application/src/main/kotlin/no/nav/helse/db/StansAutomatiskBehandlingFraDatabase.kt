package no.nav.helse.db

import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import java.time.LocalDateTime

data class StansAutomatiskBehandlingFraDatabase(
    val fødselsnummer: String,
    val status: String,
    val årsaker: Set<StoppknappÅrsak>,
    val opprettet: LocalDateTime,
    val meldingId: String?,
)
