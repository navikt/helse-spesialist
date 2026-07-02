package no.nav.helse.spesialist.client.sparkel.sykepengeperioder

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import no.nav.helse.spesialist.application.Cache

class ClientSparkelSykepengeperioderModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
    cache: Cache,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val sykepengeperioderHenter =
        SparkelSykepengeperioderClient(
            configuration = configuration,
            accessTokenProvider = accessTokenProvider,
            cache = cache,
        )
}
