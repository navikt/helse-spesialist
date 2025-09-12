package no.nav.helse.spesialist.api.saksbehandler

import io.ktor.server.auth.jwt.JWTPrincipal
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgrupper
import java.util.UUID

data class SaksbehandlerFraApi(
    val oid: UUID,
    val navn: String,
    val epost: String,
    val ident: String,
    val grupper: List<UUID>,
    val tilgangsgrupper: Set<Tilgangsgruppe>,
) {
    companion object {
        fun fraOnBehalfOfToken(
            jwtPrincipal: JWTPrincipal,
            tilgangsgrupper: Tilgangsgrupper,
        ): SaksbehandlerFraApi {
            val gruppeUuider =
                jwtPrincipal.payload
                    .getClaim("groups")
                    ?.asList(String::class.java)
                    ?.map(UUID::fromString) ?: emptyList()
            return SaksbehandlerFraApi(
                epost = jwtPrincipal.payload.getClaim("preferred_username").asString(),
                oid =
                    jwtPrincipal.payload
                        .getClaim("oid")
                        .asString()
                        .let { UUID.fromString(it) },
                navn = jwtPrincipal.payload.getClaim("name").asString(),
                ident = jwtPrincipal.payload.getClaim("NAVident").asString(),
                grupper = gruppeUuider,
                tilgangsgrupper = tilgangsgrupper.grupperFor(gruppeUuider.toSet()),
            )
        }
    }
}
