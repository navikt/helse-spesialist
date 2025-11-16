package no.nav.helse.spesialist.application

import no.nav.helse.db.VedtakBegrunnelseRepository
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.VedtakBegrunnelseId

class InMemoryVedtakBegrunnelseRepository : VedtakBegrunnelseRepository,
    AbstractInMemoryRepository<VedtakBegrunnelseId, VedtakBegrunnelse>() {
    override fun finn(spleisBehandlingId: SpleisBehandlingId): VedtakBegrunnelse? =
        alle().find { it.spleisBehandlingId == spleisBehandlingId }

    override fun generateId(): VedtakBegrunnelseId =
        VedtakBegrunnelseId((alle().maxOfOrNull { it.id().value } ?: 0) + 1)

    override fun deepCopy(original: VedtakBegrunnelse): VedtakBegrunnelse = VedtakBegrunnelse.fraLagring(
        id = original.id(),
        spleisBehandlingId = original.spleisBehandlingId,
        tekst = original.tekst,
        utfall = original.utfall,
        invalidert = original.invalidert,
        saksbehandlerOid = original.saksbehandlerOid,
    )
}
