package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID

data class VarseldefinisjonDto(
    val id: UUID,
    val varselkode: String,
    val tittel: String,
    val forklaring: String?,
    val handling: String?,
    val avviklet: Boolean,
    val opprettet: LocalDateTime,
)
