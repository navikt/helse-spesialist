package no.nav.helse.spesialist.client.tilgangsmaskinen

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.client.TilgangsmaskinenClient

class ClientTilgangsmaskinenModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
) {
    data class Configuration(
        val scope: String,
        val baseUrl: String,
    )

    val tilgangsmaskinenClient: PopulasjonstilgangskontrollProvider =
        TilgangsmaskinenClient(
            scope = configuration.scope,
            baseUrl = configuration.baseUrl,
            tokenProvider = accessTokenProvider,
        )
}
