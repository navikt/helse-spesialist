package no.nav.helse.spesialist.application

import no.nav.helse.db.IndividuellBegrunnelseRepository
import no.nav.helse.spesialist.domain.IndividuellBegrunnelse
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtakBegrunnelseId

class InMemoryIndividuellBegrunnelseRepository :
    AbstractLateIdInMemoryRepository<VedtakBegrunnelseId, IndividuellBegrunnelse>(),
    IndividuellBegrunnelseRepository {
    override fun finn(spleisBehandlingId: SpleisBehandlingId): IndividuellBegrunnelse? = alle().find { it.spleisBehandlingId == spleisBehandlingId }

    override fun tildelIderSomMangler(root: IndividuellBegrunnelse) {
        if (!root.harFåttTildeltId()) {
            root.tildelId(VedtakBegrunnelseId((alle().maxOfOrNull { it.id().value } ?: 0) + 1))
        }
    }

    override fun deepCopy(original: IndividuellBegrunnelse): IndividuellBegrunnelse =
        IndividuellBegrunnelse.fraLagring(
            id = original.id(),
            spleisBehandlingId = original.spleisBehandlingId,
            tekst = original.tekst,
            utfall = original.utfall,
            invalidert = original.invalidert,
            saksbehandlerOid = original.saksbehandlerOid,
        )
}
