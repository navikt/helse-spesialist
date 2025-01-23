package no.nav.helse.mediator.påvent

import no.nav.helse.db.PåVentDao
import no.nav.helse.modell.saksbehandler.handlinger.EndrePåVent
import no.nav.helse.modell.saksbehandler.handlinger.LeggPåVent
import java.util.UUID

class PåVentRepository(
    private val dao: PåVentDao,
) {
    internal fun leggPåVent(
        saksbehandlerOid: UUID,
        påVent: LeggPåVent,
        dialogRef: Long,
    ) {
        dao.lagrePåVent(påVent.oppgaveId, saksbehandlerOid, påVent.frist, påVent.årsaker, påVent.notatTekst, dialogRef)
    }

    internal fun fjernFraPåVent(oppgaveId: Long) {
        dao.slettPåVent(oppgaveId)
    }

    internal fun endrePåVent(
        saksbehandlerOid: UUID,
        endrePåVent: EndrePåVent,
        dialogRef: Long,
    ) {
        dao.oppdaterPåVent(
            endrePåVent.oppgaveId,
            saksbehandlerOid,
            endrePåVent.frist,
            endrePåVent.årsaker,
            endrePåVent.notatTekst,
            dialogRef,
        )
    }
}
