package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak

class InMemoryVedtakRepository :
    AbstractInMemoryRepository<SpleisBehandlingId, Vedtak>(),
    VedtakRepository {
    override fun deepCopy(original: Vedtak): Vedtak =
        Vedtak.fraLagring(
            id = original.id,
            automatiskFattet = original.automatiskFattet,
            saksbehandlerIdent = original.saksbehandlerIdent,
            beslutterIdent = original.beslutterIdent,
            tidspunkt = original.tidspunkt,
        )
}
