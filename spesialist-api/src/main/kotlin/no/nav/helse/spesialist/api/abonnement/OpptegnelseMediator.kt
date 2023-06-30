package no.nav.helse.spesialist.api.abonnement

import java.util.UUID

class OpptegnelseMediator(private val opptegnelseDao: OpptegnelseDao, private val abonnementDao: AbonnementDao) {
    internal fun opprettAbonnement(saksbehandlerIdent: UUID, person_id: Long) {
        abonnementDao.opprettAbonnement(saksbehandlerIdent, person_id)
    }

    fun hentAbonnerteOpptegnelser(saksbehandlerIdent: UUID, sisteSekvensId: Int): List<OpptegnelseDto> {
        abonnementDao.registrerSistekvensnummer(saksbehandlerIdent, sisteSekvensId)
        return opptegnelseDao.finnOpptegnelser(saksbehandlerIdent).map {
            OpptegnelseDto(
                it.aktorId.toLong(),
                it.sekvensnummer,
                OpptegnelseType.valueOf(it.type.toString()),
                it.payload
            )
        }
    }

    internal fun hentAbonnerteOpptegnelser(saksbehandlerIdent: UUID): List<OpptegnelseDto> {
        return opptegnelseDao.finnOpptegnelser(saksbehandlerIdent).map {
            OpptegnelseDto(
                it.aktorId.toLong(),
                it.sekvensnummer,
                OpptegnelseType.valueOf(it.type.toString()),
                it.payload
            )
        }
    }
}
