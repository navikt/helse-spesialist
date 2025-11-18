package no.nav.helse.db

import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId

interface BehandlingRepository {
    fun finn(id: SpleisBehandlingId): Behandling?

    fun finn(id: BehandlingUnikId): Behandling?

    fun finnAndreBehandlingerISykefraværstilfelle(
        behandling: Behandling,
        fødselsnummer: String,
    ): Set<Behandling>

    fun finnNyesteForVedtaksperiode(vedtaksperiodeId: VedtaksperiodeId): Behandling?

    fun lagre(behandling: Behandling)

    fun lagreAlle(behandlinger: Collection<Behandling>)
}
