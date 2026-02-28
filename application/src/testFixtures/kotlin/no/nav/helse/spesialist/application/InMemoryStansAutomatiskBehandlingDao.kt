package no.nav.helse.spesialist.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
import java.time.LocalDateTime

class InMemoryStansAutomatiskBehandlingDao : StansAutomatiskBehandlingDao {
    private val data = mutableListOf<StansAutomatiskBehandlingFraDatabase>()

    override fun hentFor(fødselsnummer: String) =
        data.filter { it.fødselsnummer == fødselsnummer }

    override fun lagreFraISyfo(melding: StansAutomatiskBehandlingMelding) {
        data.add(
            StansAutomatiskBehandlingFraDatabase(
                fødselsnummer = melding.fødselsnummer(),
                status = melding.status,
                årsaker = melding.årsaker,
                opprettet = melding.opprettet,
                meldingId = jacksonObjectMapper().readTree(melding.originalMelding).get("uuid")?.asText()
            )
        )
    }

    override fun lagreFraSpeil(fødselsnummer: String) {
        data.add(
            StansAutomatiskBehandlingFraDatabase(
                fødselsnummer = fødselsnummer,
                status = "NORMAL",
                årsaker = emptySet(),
                opprettet = LocalDateTime.now(),
                meldingId = null
            )
        )
    }
}
