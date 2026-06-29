package no.nav.helse.spesialist.client.spforsikring

import no.nav.helse.spesialist.application.AccessTokenGenerator

class ClientSpForsikringModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val spForsikringClientForsikringHenter =
        SpForsikringClientForsikringHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
        )
}
