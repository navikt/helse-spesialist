package no.nav.helse.spesialist.api.saksbehandler

import io.ktor.server.auth.jwt.JWTPrincipal
import java.util.UUID

data class SaksbehandlerDto(
    val oid: UUID,
    val navn: String,
    val epost: String,
    val ident: String
) {
    companion object {
        fun fraOnBehalfOfToken(jwtPrincipal: JWTPrincipal) = SaksbehandlerDto(
            oid = jwtPrincipal.payload.getClaim("oid").asString().let { UUID.fromString(it) },
            navn = jwtPrincipal.payload.getClaim("name").asString(),
            epost = jwtPrincipal.payload.getClaim("preferred_username").asString(),
            ident = jwtPrincipal.payload.getClaim("NAVident").asString(),
        )
    }
}
