package no.nav.helse.spesialist.client.entraid

import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller

class ClientEntraIDModule(
    configuration: Configuration,
    tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
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
            msGraphUrl = configuration.msGraphUrl,
            tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
        )
}
