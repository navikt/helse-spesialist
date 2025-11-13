package no.nav.helse.db

import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId

interface BehandlingRepository {
    fun finn(id: SpleisBehandlingId): Behandling?

    fun finn(id: BehandlingUnikId): Behandling?

    fun finnAndreBehandlingerISykefraværstilfelle(
        behandling: Behandling,
        fødselsnummer: String,
    ): Set<Behandling>

    fun lagre(behandling: Behandling)
}
