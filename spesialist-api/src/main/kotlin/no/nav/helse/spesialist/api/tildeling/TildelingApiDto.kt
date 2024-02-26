package no.nav.helse.spesialist.api.tildeling

import java.util.UUID

data class TildelingApiDto(
    val navn: String,
    val epost: String,
    val oid: UUID,
)
