package no.nav.helse.spesialist.client.sparkel.sykepengeperioder

import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Cache

class ClientSparkelSykepengeperioderModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
    cache: Cache,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val sykepengeperioderHenter =
        SparkelSykepengeperioderClient(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
            cache = cache,
        )
}
