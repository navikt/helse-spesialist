package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import java.util.UUID

class InMemoryNotatRepository : NotatRepository, AbstractInMemoryRepository<NotatId, Notat>() {
    override fun finnAlleForVedtaksperiode(vedtaksperiodeId: UUID): List<Notat> =
        alle().filter { it.vedtaksperiodeId == vedtaksperiodeId }

    override fun generateId(): NotatId = NotatId((alle().maxOfOrNull { it.id().value } ?: 0) + 1)
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
