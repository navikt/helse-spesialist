package no.nav.helse.db

import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.util.UUID

interface BehandlingRepository {
    fun finn(id: SpleisBehandlingId): Behandling?

    fun finnNyeste(vedtaksperiodeId: UUID): Behandling?
}
