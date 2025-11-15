package no.nav.helse.spesialist.application

import no.nav.helse.db.BehandlingRepository
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.util.UUID

class InMemoryBehandlingRepository : BehandlingRepository, AbstractInMemoryRepository<BehandlingUnikId, Behandling>() {
    override fun generateId(): BehandlingUnikId = BehandlingUnikId(UUID.randomUUID())
    override fun finn(id: SpleisBehandlingId): Behandling? = alle().firstOrNull { it.spleisBehandlingId == id }
    override fun finnAndreBehandlingerISykefraværstilfelle(
        behandling: Behandling,
        fødselsnummer: String
    ): Set<Behandling> =
        alle()
            .filter { it.skjæringstidspunkt.isEqual(behandling.skjæringstidspunkt) }
            .toSet()
}
