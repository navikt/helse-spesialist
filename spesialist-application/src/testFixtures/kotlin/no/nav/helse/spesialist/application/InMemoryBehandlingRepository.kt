package no.nav.helse.spesialist.application

import no.nav.helse.db.BehandlingRepository
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class InMemoryBehandlingRepository : BehandlingRepository, AbstractInMemoryRepository<BehandlingUnikId, Behandling>() {
    override fun finn(id: SpleisBehandlingId): Behandling? = alle().firstOrNull { it.spleisBehandlingId == id }
    override fun finnAndreBehandlingerISykefraværstilfelle(
        behandling: Behandling,
        fødselsnummer: String
    ): Set<Behandling> =
        alle()
            .filter { it.skjæringstidspunkt.isEqual(behandling.skjæringstidspunkt) }
            .toSet()

    override fun deepCopy(original: Behandling): Behandling = Behandling.fraLagring(
        id = original.id,
        spleisBehandlingId = original.spleisBehandlingId,
        vedtaksperiodeId = original.vedtaksperiodeId,
        utbetalingId = original.utbetalingId,
        tags = original.tags.toSet(),
        tilstand = original.tilstand,
        fom = original.fom,
        tom = original.tom,
        skjæringstidspunkt = original.skjæringstidspunkt,
        yrkesaktivitetstype = original.yrkesaktivitetstype,
        søknadIder = original.søknadIder().toSet(),
    )
}
