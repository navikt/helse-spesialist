package no.nav.helse.spesialist.client.sparkel.norg

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import no.nav.helse.spesialist.application.Cache

class ClientSparkelNorgModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
    cache: Cache,
) {
    data class Configuration(
        val apiUrl: String,
        val scope: String,
    )

    val behandlendeEnhetHenter =
        SparkelNorgClientBehandlendeEnhetHenter(
            configuration = configuration,
            accessTokenProvider = accessTokenProvider,
            cache = cache,
        )
}
