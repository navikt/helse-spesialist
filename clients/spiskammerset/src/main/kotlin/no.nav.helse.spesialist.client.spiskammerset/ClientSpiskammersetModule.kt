package no.nav.helse.spesialist.client.spiskammerset

import no.nav.helse.spesialist.application.AccessTokenGenerator

class ClientSpiskammersetModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val spiskammersetClientForsikringHenter =
        SpiskammersetClientForsikringHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
        )
}
