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

    // GraphQL-klienten (SpleisClient/SpleisClientSnapshothenter) og skygge-sammenligningen
    // (SnapshotSammenligningHenter) er ikke lenger koblet inn - migreringen til spleis sitt
    // REST-endepunkt (`POST /api/person`) er fullført, og den gamle koden ligger død inntil videre.
    val snapshothenter: Snapshothenter =
        SpleisRestClientSnapshothenter(
            SpleisRestClient(
                accessTokenProvider = accessTokenProvider,
                spleisUrl = configuration.spleisUrl,
                spleisClientId = configuration.spleisClientId,
            ),
        )
}
