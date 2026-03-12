package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.util.UUID

class InMemoryNotatRepository : NotatRepository, AbstractLateIdInMemoryRepository<NotatId, Notat>() {
    override fun finnAlleForVedtaksperiode(vedtaksperiodeId: UUID): List<Notat> =
        alle().filter { it.vedtaksperiodeId == vedtaksperiodeId }

    override fun finnAlleForVedtaksperioder(vedtaksperiodeIds: Set<VedtaksperiodeId>): List<Notat> =
        alle().filter { VedtaksperiodeId(it.vedtaksperiodeId) in vedtaksperiodeIds }

    override fun tildelIderSomMangler(root: Notat) {
        if (!root.harFåttTildeltId())
            root.tildelId(NotatId((alle().maxOfOrNull { it.id().value } ?: 0) + 1))
    }

    override fun deepCopy(original: Notat): Notat = Notat.Factory.fraLagring(
        id = original.id(),
        type = original.type,
        tekst = original.tekst,
        dialogRef = original.dialogRef,
        vedtaksperiodeId = original.vedtaksperiodeId,
        saksbehandlerOid = original.saksbehandlerOid,
        opprettetTidspunkt = original.opprettetTidspunkt,
        feilregistrert = original.feilregistrert,
        feilregistrertTidspunkt = original.feilregistrertTidspunkt,
    )
}
