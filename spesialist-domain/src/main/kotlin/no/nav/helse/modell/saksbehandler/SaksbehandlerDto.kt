package no.nav.helse.modell.saksbehandler

import java.util.UUID

data class SaksbehandlerDto(
    val epostadresse: String,
    val oid: UUID,
    val navn: String,
    val ident: String,
)
