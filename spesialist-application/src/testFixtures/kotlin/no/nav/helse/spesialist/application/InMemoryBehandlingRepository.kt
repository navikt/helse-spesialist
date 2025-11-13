package no.nav.helse.spesialist.application

import no.nav.helse.db.BehandlingRepository
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class InMemoryBehandlingRepository : BehandlingRepository {
    val behandlinger = mutableMapOf<SpleisBehandlingId, Behandling>()

    override fun finn(id: SpleisBehandlingId): Behandling? {
        return behandlinger[id]
    }

    override fun finn(id: BehandlingUnikId): Behandling? {
        return behandlinger.values.find { it.id() == id }
    }

    override fun finnAndreBehandlingerISykefraværstilfelle(
        behandling: Behandling,
        fødselsnummer: String
    ): Set<Behandling> {
        return behandlinger.values.filter { it.skjæringstidspunkt.isEqual(behandling.skjæringstidspunkt) }.toSet()
    }

    override fun lagre(behandling: Behandling) {
        behandlinger[behandling.spleisBehandlingId!!] = behandling
    }
}
