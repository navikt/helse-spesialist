package no.nav.helse.spesialist.client.speed

import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Cache

class ClientSpeedModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
    cache: Cache,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val alleIdenterHenter =
        SpeedClientAlleIdenterHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
            cache = cache,
        )

    val personinfoHenter =
        SpeedClientPersoninfoHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
            cache = cache,
        )
}
