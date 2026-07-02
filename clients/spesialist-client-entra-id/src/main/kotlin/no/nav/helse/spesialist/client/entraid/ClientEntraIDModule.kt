package no.nav.helse.spesialist.client.entraid

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import com.github.navikt.tbd_libs.access_token.TexasClient
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import java.net.URI

class ClientEntraIDModule(
    configuration: Configuration,
    tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
) {
    data class Configuration(
        val tokenEndpoint: String,
        val tokenExchangeEndpoint: String,
        val msGraphUrl: String,
    )

    val accessTokenProvider: AccessTokenProvider =
        TexasClient(
            tokenEndpoint = URI(configuration.tokenEndpoint),
            tokenExchangeEndpoint = URI(configuration.tokenExchangeEndpoint),
        )

    val tilgangsgruppehenter =
        MsGraphTilgangsgruppehenter(
            accessTokenProvider = accessTokenProvider,
            msGraphUrl = configuration.msGraphUrl,
            tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
        )
}
