package no.nav.helse.db

import java.util.UUID

data class SaksbehandlerFraDatabase(
    val epostadresse: String,
    val oid: UUID,
    val navn: String,
    val ident: String,
)
