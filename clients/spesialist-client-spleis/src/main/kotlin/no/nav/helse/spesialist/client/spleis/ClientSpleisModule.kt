package no.nav.helse.spesialist.client.spleis

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.spesialist.application.Snapshothenter
import java.net.URI

class ClientSpleisModule(
    configuration: Configuration,
    accessTokenProvider: AccessTokenProvider,
    environmentToggles: EnvironmentToggles,
) {
    data class Configuration(
        val spleisUrl: URI,
        val spleisClientId: String,
        val loggRespons: Boolean,
    )

    val snapshothenter: Snapshothenter =
        SnapshotSammenligningHenter(
            graphQL =
                SpleisClientSnapshothenter(
                    SpleisClient(
                        accessTokenProvider = accessTokenProvider,
                        spleisUrl = configuration.spleisUrl,
                        spleisClientId = configuration.spleisClientId,
                        loggRespons = configuration.loggRespons,
                    ),
                ),
            hentPersonRest =
                SpleisRestClient(
                    accessTokenProvider = accessTokenProvider,
                    spleisUrl = configuration.spleisUrl,
                    spleisClientId = configuration.spleisClientId,
                )::hentPerson,
            // Kjøres foreløpig kun i dev-gcp - se plan for gradvis utrulling til prod-gcp.
            skalSammenligne = environmentToggles.devGcp,
        )
}
