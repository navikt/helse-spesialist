package no.nav.helse.spesialist.client.krr

import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Cache

class ClientKrrModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
    cache: Cache,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val krrRegistrertStatusHenter =
        KRRClientKrrRegistrertStatusHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
            cache = cache,
        )
}
