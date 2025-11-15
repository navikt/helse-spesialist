package no.nav.helse.spesialist.application

import no.nav.helse.db.OpptegnelseDao
import java.util.UUID

class InMemoryOpptegnelseDao(
    private val personRepository: InMemoryPersonRepository,
    private val abonnementApiDao: InMemoryAbonnementApiDao,
) : OpptegnelseDao {
    private val data = mutableListOf<OpptegnelseDao.Opptegnelse>()

    override fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: String,
        type: OpptegnelseDao.Opptegnelse.Type
    ) {
        data.add(
            OpptegnelseDao.Opptegnelse(
                aktorId = personRepository.alle().first { it.identitetsnummer.value == fødselsnummer }.aktørId,
                sekvensnummer = (data.maxOfOrNull { it.sekvensnummer } ?: 0) + 1,
                type = type,
                payload = payload
            )
        )
    }

    override fun finnOpptegnelser(saksbehandlerIdent: UUID): List<OpptegnelseDao.Opptegnelse> =
        data
            .filter { it.sekvensnummer > abonnementApiDao.sisteSekvensnummerMap.getOrDefault(saksbehandlerIdent, 0) }
            .filter { it.aktorId in abonnementApiDao.abonnementMap.getOrDefault(saksbehandlerIdent, emptySet()) }
}
