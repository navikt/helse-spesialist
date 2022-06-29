package no.nav.helse.spesialist.api.saksbehandler

import java.util.*

data class SaksbehandlerDto(
    val oid: UUID,
    val navn: String,
    val epost: String,
    val ident: String
)
