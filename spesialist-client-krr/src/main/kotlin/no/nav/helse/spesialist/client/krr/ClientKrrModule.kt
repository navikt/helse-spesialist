package no.nav.helse.spesialist.client.krr

import no.nav.helse.spesialist.application.AccessTokenGenerator

class ClientKrrModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val reservasjonshenter =
        KRRClientReservasjonshenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
        )
}
