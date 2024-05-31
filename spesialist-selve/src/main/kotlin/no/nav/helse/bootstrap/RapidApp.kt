package no.nav.helse.bootstrap

import no.nav.helse.MsGraphClient
import no.nav.helse.SpeilTilgangsgrupper
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.client.AccessTokenClient
import no.nav.helse.spesialist.api.reservasjon.KRRClient
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import java.net.URI

internal class RapidApp(env: Map<String, String>) {
    private lateinit var rapidsConnection: RapidsConnection
    private val azureConfig =
        AzureConfig(
            clientId = env.getValue("AZURE_APP_CLIENT_ID"),
            issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
            jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
            tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        )
    private val accessTokenClient =
        AccessTokenClient(
            httpClient = azureAdClient(),
            azureConfig = azureConfig,
            privateJwk = env.getValue("AZURE_APP_JWK"),
        )
    private val snapshotClient =
        SnapshotClient(
            httpClient = httpClient(120_000, 1_000, 40_000),
            accessTokenClient = accessTokenClient,
            spleisUrl = URI.create(env.getValue("SPLEIS_API_URL")),
            spleisClientId = env.getValue("SPLEIS_CLIENT_ID"),
        )
    private val reservasjonClient =
        KRRClient(
            httpClient = httpClient(1_000, 1_000, 2_000),
            accessTokenClient = accessTokenClient,
            apiUrl = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_API_URL"),
            scope = env.getValue("KONTAKT_OG_RESERVASJONSREGISTERET_SCOPE"),
        )

    private val msGraphClient = MsGraphClient(httpClient(120_000, 1_000, 40_000), accessTokenClient)

    private val tilgangsgrupper = SpeilTilgangsgrupper(System.getenv())
    private val spesialistApp =
        SpesialistApp(
            env = System.getenv(),
            gruppekontroll = msGraphClient,
            snapshotClient = snapshotClient,
            azureConfig = azureConfig,
            tilgangsgrupper = tilgangsgrupper,
            reservasjonClient = reservasjonClient,
            versjonAvKode = versjonAvKode(env),
        ) {
            rapidsConnection
        }

    private fun versjonAvKode(env: Map<String, String>): String {
        return env["NAIS_APP_IMAGE"] ?: throw IllegalArgumentException("NAIS_APP_IMAGE env variable is missing")
    }

    init {
        rapidsConnection =
            RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule {
                spesialistApp.ktorApp(this)
            }.build()
    }

    fun start() = spesialistApp.start()
}
