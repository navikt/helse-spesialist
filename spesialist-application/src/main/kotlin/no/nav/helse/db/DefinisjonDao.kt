package no.nav.helse.db

import no.nav.helse.modell.varsel.Varseldefinisjon
import java.time.LocalDateTime
import java.util.UUID

interface DefinisjonDao {
    fun definisjonFor(definisjonId: UUID): Varseldefinisjon

    fun sisteDefinisjonFor(varselkode: String): Varseldefinisjon

    fun lagreDefinisjon(
        unikId: UUID,
        kode: String,
        tittel: String,
        forklaring: String?,
        handling: String?,
        avviklet: Boolean,
        opprettet: LocalDateTime,
    )
}
