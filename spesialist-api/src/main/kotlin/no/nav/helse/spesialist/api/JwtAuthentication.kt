package no.nav.helse.spesialist.api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.request.uri
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import java.net.URI
import java.util.UUID

fun Application.jwtAuthentication(
    config: ApiModule.Configuration,
    tilgangsgruppeUuider: TilgangsgruppeUuider,
    tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
) {
    jwtAuthentication(
        jwkProvider = JwkProviderBuilder(URI(config.jwkProviderUri).toURL()).build(),
        issuerUrl = config.issuerUrl,
        clientId = config.clientId,
        tilgangsgruppeUuider = tilgangsgruppeUuider,
        tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
    )
}

fun Application.jwtAuthentication(
    jwkProvider: JwkProvider,
    issuerUrl: String,
    clientId: String,
    tilgangsgruppeUuider: TilgangsgruppeUuider,
    tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
) {
    authentication {
        jwt("oidc") {
            skipWhen { call -> call.request.uri == "/graphql/playground" }
            verifier(jwkProvider, issuerUrl) {
                withAudience(clientId)
            }
            validate { credentials ->
                SaksbehandlerPrincipal(
                    saksbehandler = credentials.tilSaksbehandler(),
                    tilgangsgrupper = tilgangsgruppeUuider.grupperFor(credentials.groupsAsUuids()),
                    brukerroller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(credentials.groupsAsUuids()),
                )
            }
        }
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

data class SaksbehandlerPrincipal(
    val saksbehandler: Saksbehandler,
    val tilgangsgrupper: Set<Tilgangsgruppe>,
    val brukerroller: Set<Brukerrolle>,
)

fun ApplicationCall.getSaksbehandlerIdentForMdc(): String? = principal<SaksbehandlerPrincipal>()?.saksbehandler?.ident?.value
