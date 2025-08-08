package no.nav.helse.spesialist.client.spleis

import no.nav.helse.spesialist.application.AccessTokenGenerator
import java.net.URI

class ClientSpleisModule(
    configuration: Configuration,
    accessTokenGenerator: AccessTokenGenerator,
) {
    data class Configuration(
        val spleisUrl: URI,
        val spleisClientId: String,
        val loggRespons: Boolean,
    )

    val snapshothenter =
        SpleisClientSnapshothenter(
            SpleisClient(
                accessTokenGenerator = accessTokenGenerator,
                spleisUrl = configuration.spleisUrl,
                spleisClientId = configuration.spleisClientId,
                loggRespons = configuration.loggRespons,
            ),
        )
}
