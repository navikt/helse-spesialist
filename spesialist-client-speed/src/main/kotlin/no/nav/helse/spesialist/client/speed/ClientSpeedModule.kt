package no.nav.helse.spesialist.client.speed

import no.nav.helse.spesialist.application.AccessTokenGenerator

class ClientSpeedModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val historiskeIdenterHenter =
        SpeedClientHistoriskeIdenterHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
        )
}
