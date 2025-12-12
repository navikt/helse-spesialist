package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak

class InMemoryVedtakRepository :
    AbstractInMemoryRepository<SpleisBehandlingId, Vedtak>(),
    VedtakRepository {
    override fun deepCopy(original: Vedtak): Vedtak =
        when (original) {
            is Vedtak.Automatisk -> Vedtak.Automatisk(original.id, original.tidspunkt)
            is Vedtak.ManueltMedTotrinnskontroll -> Vedtak.ManueltMedTotrinnskontroll(original.id, original.tidspunkt, original.saksbehandlerIdent, original.beslutterIdent)
            is Vedtak.ManueltUtenTotrinnskontroll -> Vedtak.ManueltUtenTotrinnskontroll(original.id, original.tidspunkt, original.saksbehandlerIdent)
        }
}
