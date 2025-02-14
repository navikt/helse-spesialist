package no.nav.helse.db

import java.util.UUID

interface VarselDao {
    fun avvikleVarsel(
        varselkode: String,
        definisjonId: UUID,
    )
}
