package no.nav.helse.spesialist.client.speed

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.spesialist.application.AccessTokenGenerator

class ClientSpeedModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
    environmentToggles: EnvironmentToggles,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val historiskeIdenterHenter =
        SpeedClientHistoriskeIdenterHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
            environmentToggles = environmentToggles,
        )

    val personinfoHenter =
        SpeedClientPersoninfoHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
            environmentToggles = environmentToggles,
        )
}
