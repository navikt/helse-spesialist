package no.nav.helse.spesialist.api.tildeling

import java.util.*

data class TildelingApiDto (
    val navn: String,
    val epost: String,
    val oid: UUID,
    val påVent: Boolean
)
