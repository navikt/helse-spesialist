package no.nav.helse.mediator.påvent

import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.saksbehandler.handlinger.FjernPåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import no.nav.helse.modell.saksbehandler.handlinger.PåVent
import java.time.LocalDate
import java.util.UUID

class PåVentMediator(
    private val dao: PåVentDao,
) {
    internal fun lagre(
        påVent: PåVent,
        saksbehandlerOid: UUID,
    ) {
        when (påVent) {
            is LeggPåVent -> nyttPåVentInnslag(påVent.oppgaveId(), saksbehandlerOid, påVent.frist(), påVent.begrunnelse())
            is FjernPåVent -> fjernPåvent(påVent.oppgaveId())
        }
    }

    private fun nyttPåVentInnslag(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
        frist: LocalDate?,
        begrunnelse: String?,
    ) {
        dao.lagrePåVent(oppgaveId, saksbehandlerOid, frist, begrunnelse)
    }

    private fun fjernPåvent(oppgaveId: Long) {
        dao.slettPåVent(oppgaveId)
    }
}
