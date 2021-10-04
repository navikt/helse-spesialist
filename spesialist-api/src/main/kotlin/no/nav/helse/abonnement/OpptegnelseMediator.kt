package no.nav.helse.abonnement

import java.util.*

class OpptegnelseMediator(private val opptegnelseDao: OpptegnelseDao, private val abonnementDao: AbonnementDao) {
    fun opprettAbonnement(saksbehandlerIdent: UUID, person_id: Long) {
        abonnementDao.opprettAbonnement(saksbehandlerIdent, person_id)
    }

    fun hentAbonnerteOpptegnelser(saksbehandlerIdent: UUID, sisteSekvensId: Int): List<OpptegnelseDto> {
        abonnementDao.registrerSistekvensnummer(saksbehandlerIdent, sisteSekvensId)
        return opptegnelseDao.finnOpptegnelser(saksbehandlerIdent)
    }

    internal fun hentAbonnerteOpptegnelser(saksbehandlerIdent: UUID): List<OpptegnelseDto> {
        return opptegnelseDao.finnOpptegnelser(saksbehandlerIdent)
    }
}
