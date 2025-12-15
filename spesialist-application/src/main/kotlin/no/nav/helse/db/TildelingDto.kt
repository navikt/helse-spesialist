package no.nav.helse.db

import no.nav.helse.spesialist.domain.NAVIdent
import java.util.UUID

data class TildelingDto(
    val navn: String,
    val epost: String,
    val oid: UUID,
    val ident: NAVIdent,
)
