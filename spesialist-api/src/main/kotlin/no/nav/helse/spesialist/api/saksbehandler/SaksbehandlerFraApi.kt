package no.nav.helse.spesialist.api.saksbehandler

import io.ktor.server.auth.jwt.JWTPrincipal
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgrupper
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.util.UUID

data class SaksbehandlerFraApi(
    val oid: UUID,
    val navn: String,
    val epost: String,
    val ident: String,
    val tilgangsgrupper: Set<Tilgangsgruppe>,
) {
    fun tilSaksbehandler(): Saksbehandler =
        Saksbehandler(
            id = SaksbehandlerOid(oid),
            navn = navn,
            epost = epost,
            ident = ident,
        )

    companion object {
        fun fraOnBehalfOfToken(
            jwtPrincipal: JWTPrincipal,
            tilgangsgrupper: Tilgangsgrupper,
        ): SaksbehandlerFraApi =
            SaksbehandlerFraApi(
                oid =
                    jwtPrincipal.payload
                        .getClaim("oid")
                        .asString()
                        .let { UUID.fromString(it) },
                navn = jwtPrincipal.payload.getClaim("name").asString(),
                epost = jwtPrincipal.payload.getClaim("preferred_username").asString(),
                ident = jwtPrincipal.payload.getClaim("NAVident").asString(),
                tilgangsgrupper =
                    tilgangsgrupper.grupperFor(
                        (
                            jwtPrincipal.payload
                                .getClaim("groups")
                                ?.asList(String::class.java)
                                ?.map(UUID::fromString) ?: emptyList()
                        ).toSet(),
                    ),
            )
    }
}
