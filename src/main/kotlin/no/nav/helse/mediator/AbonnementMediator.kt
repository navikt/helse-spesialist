package no.nav.helse.mediator

import no.nav.helse.modell.abonnement.OpptegnelseDao
import no.nav.helse.modell.abonnement.OpptegnelseDto
import java.util.*

internal class AbonnementMediator(private val opptegnelseDao: OpptegnelseDao) {

    internal fun finnAbonnement(saksbehandlerIdent: UUID) = opptegnelseDao.finnAbonnement(saksbehandlerIdent)

    internal fun opprettAbonnement(
        saksbehandlerIdent: UUID,
        person_id: Long
    ) {
        opptegnelseDao.opprettAbonnement(saksbehandlerIdent, person_id)
    }

    fun hentOpptegnelser(saksbehandlerIdent: UUID, sisteSekvensId: Int): List<OpptegnelseDto> {
        opptegnelseDao.registrerSistekvensnummer(saksbehandlerIdent, sisteSekvensId)
        return opptegnelseDao.finnOpptegnelser(saksbehandlerIdent)
    }
}
