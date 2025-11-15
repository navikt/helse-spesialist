package no.nav.helse.spesialist.application

import no.nav.helse.db.api.AbonnementApiDao
import java.util.UUID

class InMemoryAbonnementApiDao : AbonnementApiDao {
    val abonnementMap = mutableMapOf<UUID, MutableSet<String>>()
    val sisteSekvensnummerMap = mutableMapOf<UUID, Int>()

    override fun opprettAbonnement(saksbehandlerId: UUID, aktørId: String) {
        abonnementMap.getOrPut(saksbehandlerId) { mutableSetOf() }.add(aktørId)
    }

    override fun registrerSistekvensnummer(
        saksbehandlerIdent: UUID,
        sisteSekvensId: Int
    ): Int {
        sisteSekvensnummerMap[saksbehandlerIdent] = sisteSekvensId
        return 1
    }
}
