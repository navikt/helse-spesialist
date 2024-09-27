package no.nav.helse.mediator.påvent

import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import java.util.UUID

class PåVentRepository(
    private val dao: PåVentDao,
) {
    internal fun leggPåVent(
        saksbehandlerOid: UUID,
        påVent: LeggPåVent,
    ) {
        dao.lagrePåVent(påVent.oppgaveId, saksbehandlerOid, påVent.frist, påVent.begrunnelse)
    }

    internal fun fjernFraPåVent(oppgaveId: Long) {
        dao.slettPåVent(oppgaveId)
    }
}
