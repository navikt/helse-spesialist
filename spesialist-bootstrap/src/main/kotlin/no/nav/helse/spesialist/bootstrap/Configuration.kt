package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.client.entraid.EntraIDAccessTokenGenerator
import no.nav.helse.spesialist.client.krr.KRRClientReservasjonshenter
import no.nav.helse.spesialist.client.spleis.SpleisClient
import no.nav.helse.spesialist.db.bootstrap.DBModule

data class Configuration(
    val azureConfig: AzureConfig,
    val accessTokenGeneratorConfig: EntraIDAccessTokenGenerator.Configuration,
    val spleisClientConfig: SpleisClient.Configuration,
    val krrConfig: KRRClientReservasjonshenter.Configuration,
    val dbConfig: DBModule.Configuration,
    val unleashFeatureToggles: UnleashFeatureToggles.Configuration,
    val versjonAvKode: String,
    val tilgangsgrupper: Tilgangsgrupper,
    val environmentToggles: EnvironmentToggles,
    val stikkprøver: Stikkprøver,
) {
    companion object {
        fun fraEnv(env: Map<String, String>): Configuration =
            Configuration(
                azureConfig = AzureConfig.fraEnv(env),
                accessTokenGeneratorConfig = EntraIDAccessTokenGenerator.Configuration.fraEnv(env),
                spleisClientConfig = SpleisClient.Configuration.fraEnv(env),
                krrConfig = KRRClientReservasjonshenter.Configuration.fraEnv(env),
                dbConfig = DBModule.Configuration.fraEnv(env),
                unleashFeatureToggles = UnleashFeatureToggles.Configuration.fraEnv(env),
                versjonAvKode = env.getValue("NAIS_APP_IMAGE"),
                tilgangsgrupper = SpeilTilgangsgrupper(env),
                environmentToggles = EnvironmentTogglesImpl(env),
                stikkprøver = Stikkprøver.fraEnv(env),
            )
    }
}
