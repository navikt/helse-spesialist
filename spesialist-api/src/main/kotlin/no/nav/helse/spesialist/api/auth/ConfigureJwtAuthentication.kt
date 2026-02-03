package no.nav.helse.spesialist.api.auth

import com.auth0.jwk.JwkProvider
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.request.uri
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID

fun JWTAuthenticationProvider.Config.configureJwtAuthentication(
    jwkProvider: JwkProvider,
    issuerUrl: String,
    clientId: String,
    tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
) {
    skipWhen { call -> call.request.uri == "/graphql/playground" }
    verifier(jwkProvider, issuerUrl) {
        withAudience(clientId)
    }
    validate { credentials ->
        SaksbehandlerPrincipal(
            saksbehandler = credentials.tilSaksbehandler(),
            brukerroller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(credentials.groupsAsUuids()),
        )
    }
}

private fun JWTCredential.groupsAsUuids(): List<UUID> =
    payload
        .getClaim("groups")
        ?.asList(String::class.java)
        ?.map(UUID::fromString)
        .orEmpty()

private fun JWTCredential.tilSaksbehandler(): Saksbehandler =
    with(payload) {
        Saksbehandler(
            id = getClaim("oid").asString().let(UUID::fromString).let(::SaksbehandlerOid),
            navn = getClaim("name").asString(),
            epost = getClaim("preferred_username").asString(),
            ident = getClaim("NAVident").asString().let(::NAVIdent),
        )
    }
