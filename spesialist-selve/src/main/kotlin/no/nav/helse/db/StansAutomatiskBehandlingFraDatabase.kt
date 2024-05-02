package no.nav.helse.db

import no.nav.helse.modell.stoppautomatiskbehandling.Status
import no.nav.helse.modell.stoppautomatiskbehandling.Årsak
import java.time.LocalDateTime

data class StansAutomatiskBehandlingFraDatabase(
    val fødselsnummer: String,
    val status: Status,
    val årsaker: Set<Årsak>,
    val opprettet: LocalDateTime,
)
