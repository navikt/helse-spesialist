package no.nav.helse.spesialist.client.spleis

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import java.net.URI

class ClientSpleisModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
) {
    data class Configuration(
        val spleisUrl: URI,
        val spleisClientId: String,
        val loggRespons: Boolean,
    )

    val snapshothenter =
        SpleisClientSnapshothenter(
            SpleisClient(
                accessTokenProvider = accessTokenProvider,
                spleisUrl = configuration.spleisUrl,
                spleisClientId = configuration.spleisClientId,
                loggRespons = configuration.loggRespons,
            ),
        )
}
