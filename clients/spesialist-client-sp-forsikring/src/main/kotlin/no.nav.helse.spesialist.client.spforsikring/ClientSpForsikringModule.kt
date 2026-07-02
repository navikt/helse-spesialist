package no.nav.helse.spesialist.client.spforsikring

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider

class ClientSpForsikringModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val spForsikringClientForsikringsvurderingHenter =
        SpForsikringClientForsikringsvurderingHenter(
            configuration = configuration,
            accessTokenProvider = accessTokenProvider,
        )
}
