package no.nav.helse.db

import java.util.UUID

data class TildelingDto(
    val navn: String,
    val epost: String,
    val oid: UUID,
)
