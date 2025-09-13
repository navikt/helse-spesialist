package no.nav.helse.spesialist.client.entraid

import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgruppeUuider

class ClientEntraIDModule(
    configuration: Configuration,
    tilgangsgruppeUuider: TilgangsgruppeUuider,
) {
    data class Configuration(
        val clientId: String,
        val tokenEndpoint: String,
        val privateJwk: String,
        val msGraphUrl: String,
    )

    val accessTokenGenerator =
        EntraIDAccessTokenGenerator(
            clientId = configuration.clientId,
            tokenEndpoint = configuration.tokenEndpoint,
            privateJwk = configuration.privateJwk,
        )

    val tilgangsgruppehenter =
        MsGraphTilgangsgruppehenter(
            accessTokenGenerator = accessTokenGenerator,
            tilgangsgruppeUuider = tilgangsgruppeUuider,
            msGraphUrl = configuration.msGraphUrl,
        )
}
