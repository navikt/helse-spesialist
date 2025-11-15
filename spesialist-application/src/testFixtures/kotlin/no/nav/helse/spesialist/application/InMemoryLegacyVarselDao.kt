package no.nav.helse.spesialist.application

import no.nav.helse.db.LegacyVarselDao
import java.util.UUID

class InMemoryLegacyVarselDao : LegacyVarselDao {
    override fun avvikleVarsel(varselkode: String, definisjonId: UUID) {
        TODO("Not yet implemented")
    }
}
