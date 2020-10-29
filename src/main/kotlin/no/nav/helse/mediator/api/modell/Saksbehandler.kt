package no.nav.helse.mediator.api.modell

import io.ktor.auth.jwt.*
import no.nav.helse.objectMapper
import java.util.*

internal class Saksbehandler(
    private val epostadresse: String,
    private val oid: UUID,
    private val ident: String,
    private val navn: String
) {
    companion object {
        fun fraToken(jwtPrincipal: JWTPrincipal): Saksbehandler {
            return Saksbehandler(
                oid = jwtPrincipal.payload.getClaim("oid").asString().let { UUID.fromString(it) },
                epostadresse = jwtPrincipal.payload.getClaim("preferred_username").asString(),
                ident = jwtPrincipal.payload.getClaim("NAVident").asString(),
                navn = jwtPrincipal.payload.getClaim("name").asString(),
            )
        }

        fun fraOnBehalfOfToken(jwtPrincipal: JWTPrincipal, ident: String) = Saksbehandler(
            oid = jwtPrincipal.payload.getClaim("oid").asString().let { UUID.fromString(it) },
            epostadresse = jwtPrincipal.payload.getClaim("preferred_username").asString(),
            ident = ident,
            navn = jwtPrincipal.payload.getClaim("name").asString(),
        )
    }

    internal fun json() = mapOf(
        "epostaddresse" to epostadresse,
        "oid" to oid,
        "ident" to ident,
        "navn" to navn,
    )
}
