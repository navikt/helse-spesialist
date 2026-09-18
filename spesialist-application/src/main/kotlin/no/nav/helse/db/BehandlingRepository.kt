package no.nav.helse.db

import no.nav.helse.spesialist.domain.*
import no.nav.helse.spesialist.domain.Behandling.Companion.behandlingspakke

interface BehandlingRepository {
    fun finnOrNull(id: SpleisBehandlingId): Behandling?

    fun finn(id: SpleisBehandlingId): Behandling = finnOrNull(id) ?: error("Fant ikke behandling med spleisBehandlingId ${id.value}")

    fun finnOrNull(id: BehandlingUnikId): Behandling?

    fun finnAndreBehandlingerISykefraværstilfelle(
        behandling: Behandling,
        fødselsnummer: String,
    ): Set<Behandling>

    fun finnBehandlingspakke(
        behandling: Behandling,
        fødselsnummer: String,
    ): Set<Behandling> =
        finnAndreBehandlingerISykefraværstilfelle(behandling, fødselsnummer)
            .behandlingspakke(behandling)

    fun finnNyesteForVedtaksperiode(vedtaksperiodeId: VedtaksperiodeId): Behandling?

    fun finnAlle(identitetsnummer: Identitetsnummer): List<Behandling>

    fun finnNyeste(identitetsnummer: Identitetsnummer): Behandling?

    fun lagre(behandling: Behandling)

    fun lagreAlle(behandlinger: Collection<Behandling>)
}
