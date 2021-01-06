package no.nav.helse.mediator

import no.nav.helse.modell.abonnement.OpptegnelseDao
import no.nav.helse.modell.abonnement.OpptegnelseDto
import java.util.*

internal class OpptegnelseMediator(private val opptegnelseDao: OpptegnelseDao) {
    internal fun opprettAbonnement(
        saksbehandlerIdent: UUID,
        person_id: Long
    ) {
        opptegnelseDao.opprettAbonnement(saksbehandlerIdent, person_id)
    }

    fun hentAbonnerteOpptegnelser(saksbehandlerIdent: UUID, sisteSekvensId: Int): List<OpptegnelseDto> {
        opptegnelseDao.registrerSistekvensnummer(saksbehandlerIdent, sisteSekvensId)
        return opptegnelseDao.finnOpptegnelser(saksbehandlerIdent)
    }
    fun hentAbonnerteOpptegnelser(saksbehandlerIdent: UUID): List<OpptegnelseDto> {
        return opptegnelseDao.finnOpptegnelser(saksbehandlerIdent)
    }
}
