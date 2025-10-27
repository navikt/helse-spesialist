package no.nav.helse.spesialist.application

import no.nav.helse.db.BehandlingRepository
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.time.LocalDate

class InMemoryBehandlingRepository : BehandlingRepository {
    val behandlinger = mutableMapOf<SpleisBehandlingId, Behandling>()

    override fun finn(id: SpleisBehandlingId): Behandling? {
        return behandlinger[id]
    }

    override fun finnBehandlingerISykefraværstilfelle(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate
    ): List<Behandling> {
        TODO("Not yet implemented")
    }

    override fun lagre(behandling: Behandling) {
        behandlinger[behandling.id] = behandling
    }
}
