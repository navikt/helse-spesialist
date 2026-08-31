package no.nav.helse.spesialist.client.spleis

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import no.nav.helse.spesialist.application.Snapshothenter
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

    val snapshothenter: Snapshothenter =
        SpleisRestClientSnapshothenter(
            SpleisRestClient(
                accessTokenProvider = accessTokenProvider,
                spleisUrl = configuration.spleisUrl,
                spleisClientId = configuration.spleisClientId,
            ),
        )
}
