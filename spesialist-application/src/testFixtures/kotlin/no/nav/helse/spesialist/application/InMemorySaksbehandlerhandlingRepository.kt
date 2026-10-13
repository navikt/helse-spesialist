package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling
import no.nav.helse.spesialist.domain.saksbehandlerhandling.SaksbehandlerhandlingId

class InMemorySaksbehandlerhandlingRepository :
    AbstractInMemoryRepository<SaksbehandlerhandlingId, Saksbehandlerhandling>(),
    SaksbehandlerhandlingRepository {
    override fun finnAlle(identitetsnummer: Identitetsnummer): List<Saksbehandlerhandling> =
        alle()
            .filter { it.identitetsnummer == identitetsnummer }
            .sortedByDescending { it.tidspunkt }

    override fun deepCopy(original: Saksbehandlerhandling): Saksbehandlerhandling =
        Saksbehandlerhandling.fraLagring(
            id = original.id,
            identitetsnummer = original.identitetsnummer,
            handlingId = original.handlingId,
            handlingType = original.handlingType,
            saksbehandlerIdent = original.saksbehandlerIdent,
            tidspunkt = original.tidspunkt,
            skjæringstidspunkt = original.skjæringstidspunkt,
            behandlingId = original.behandlingId,
            totrinnsvurderingId = original.totrinnsvurderingId,
        )
}
