package no.nav.helse.db

import java.util.UUID

interface LegacyVarselDao {
    fun avvikleVarsel(
        varselkode: String,
        definisjonId: UUID,
    )
}
