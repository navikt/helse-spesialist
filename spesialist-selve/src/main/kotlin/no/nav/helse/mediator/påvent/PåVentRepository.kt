package no.nav.helse.mediator.påvent

import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.saksbehandler.handlinger.FjernPåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.PåVent
import java.util.UUID

class PåVentRepository(
    private val dao: PåVentDao,
) {
    internal fun lagre(
        påVent: PåVent,
        saksbehandlerOid: UUID,
    ) = when (påVent) {
        is LeggPåVent -> nyttPåVentInnslag(saksbehandlerOid, påVent)
        is FjernPåVent -> fjernPåvent(påVent.oppgaveId)
    }

    private fun nyttPåVentInnslag(
        saksbehandlerOid: UUID,
        påVent: LeggPåVent,
    ) {
        dao.lagrePåVent(påVent.oppgaveId, saksbehandlerOid, påVent.frist, påVent.begrunnelse)
    }

    private fun fjernPåvent(oppgaveId: Long) {
        dao.slettPåVent(oppgaveId)
    }
}
