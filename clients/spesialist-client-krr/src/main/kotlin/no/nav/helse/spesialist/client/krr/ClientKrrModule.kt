package no.nav.helse.spesialist.client.krr

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import no.nav.helse.spesialist.application.Cache

class ClientKrrModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
    cache: Cache,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val krrRegistrertStatusHenter =
        KRRClientKrrRegistrertStatusHenter(
            configuration = configuration,
            accessTokenProvider = accessTokenProvider,
            cache = cache,
        )
}
