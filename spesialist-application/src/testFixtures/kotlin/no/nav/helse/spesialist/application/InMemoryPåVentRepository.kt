package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.PåVentId
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class InMemoryPåVentRepository :
    AbstractLateIdInMemoryRepository<PåVentId, PåVent>(),
    PåVentRepository {
    override fun tildelIderSomMangler(root: PåVent) {
        if (!root.harFåttTildeltId()) {
            root.tildelId(PåVentId((alle().maxOfOrNull { it.id().value } ?: 0) + 1))
        }
    }

    override fun deepCopy(original: PåVent): PåVent =
        PåVent.Factory.fraLagring(
            id = original.id(),
            vedtaksperiodeId = original.vedtaksperiodeId,
            saksbehandlerOid = original.saksbehandlerOid,
            frist = original.frist,
            opprettetTidspunkt = original.opprettetTidspunkt,
            dialogRef = original.dialogRef,
            årsaker = original.årsaker.toList(),
            notattekst = original.notattekst,
        )

    override fun finnFor(vedtaksperiodeId: VedtaksperiodeId): PåVent? = alle().firstOrNull { it.vedtaksperiodeId == vedtaksperiodeId }
}
