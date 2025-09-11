package no.nav.helse.spesialist.client.entraid

class ClientEntraIDModule(
    configuration: Configuration,
) {
    data class Configuration(
        val clientId: String,
        val tokenEndpoint: String,
        val privateJwk: String,
    )

    val accessTokenGenerator =
        EntraIDAccessTokenGenerator(
            clientId = configuration.clientId,
            tokenEndpoint = configuration.tokenEndpoint,
            privateJwk = configuration.privateJwk,
        )

    val gruppekontroll = MsGraphTilgangsgruppehenter(accessTokenGenerator)
}
