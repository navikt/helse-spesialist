package no.nav.helse.spesialist.client.speed

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import no.nav.helse.spesialist.application.Cache

class ClientSpeedModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
    cache: Cache,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val alleIdenterHenter =
        SpeedClientAlleIdenterHenter(
            configuration = configuration,
            accessTokenProvider = accessTokenProvider,
            cache = cache,
        )

    val personinfoHenter =
        SpeedClientPersoninfoHenter(
            configuration = configuration,
            accessTokenProvider = accessTokenProvider,
            cache = cache,
        )
}
