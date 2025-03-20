package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule
import no.nav.helse.spesialist.client.krr.ClientKrrModule
import no.nav.helse.spesialist.client.spleis.ClientSpleisModule
import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.kafka.KafkaModule

data class Configuration(
    val apiModuleConfiguration: ApiModule.Configuration,
    val accessTokenGeneratorConfig: ClientEntraIDModule.Configuration,
    val spleisClientConfig: ClientSpleisModule.Configuration,
    val krrConfig: ClientKrrModule.Configuration,
    val dbConfig: DBModule.Configuration,
    val kafkaModuleConfiguration: KafkaModule.Configuration,
    val unleashFeatureToggles: ClientUnleashModule.Configuration,
    val versjonAvKode: String,
    val tilgangsgrupper: Tilgangsgrupper,
    val environmentToggles: EnvironmentToggles,
    val stikkprøver: Stikkprøver,
)
