package no.nav.helse.spesialist.client.sparkel.norg

import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Cache

class ClientSparkelNorgModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
    cache: Cache,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val behandlendeEnhetHenter =
        SparkelNorgClientBehandlendeEnhetHenter(
            configuration = configuration,
            accessTokenGenerator = accessTokenGenerator,
            cache = cache,
        )
}
