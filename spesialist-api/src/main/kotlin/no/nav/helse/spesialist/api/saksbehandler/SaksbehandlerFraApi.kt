package no.nav.helse.spesialist.api.saksbehandler

import io.ktor.server.auth.jwt.JWTPrincipal
import java.util.UUID

data class SaksbehandlerFraApi(
    val oid: UUID,
    val navn: String,
    val epost: String,
    val ident: String,
    val grupper: List<UUID>,
) {
    companion object {
        fun fraOnBehalfOfToken(jwtPrincipal: JWTPrincipal) =
            SaksbehandlerFraApi(
                epost = jwtPrincipal.payload.getClaim("preferred_username").asString(),
                oid = jwtPrincipal.payload.getClaim("oid").asString().let { UUID.fromString(it) },
                navn = jwtPrincipal.payload.getClaim("name").asString(),
                ident = jwtPrincipal.payload.getClaim("NAVident").asString(),
                grupper = jwtPrincipal.payload.getClaim("groups")?.asList(String::class.java)?.map(UUID::fromString) ?: emptyList(),
            )
    }
}
