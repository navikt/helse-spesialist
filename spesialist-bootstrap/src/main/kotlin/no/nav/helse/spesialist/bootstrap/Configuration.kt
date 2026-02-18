package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.spesialist.api.ApiModule
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilTilganger
import no.nav.helse.spesialist.client.entraid.ClientEntraIDModule
import no.nav.helse.spesialist.client.krr.ClientKrrModule
import no.nav.helse.spesialist.client.spiskammerset.ClientSpiskammersetModule
import no.nav.helse.spesialist.client.spleis.ClientSpleisModule
import no.nav.helse.spesialist.db.DBModule
import no.nav.helse.spesialist.kafka.KafkaModule

data class Configuration(
    val api: ApiModule.Configuration,
    val clientEntraID: ClientEntraIDModule.Configuration,
    val clientKrr: ClientKrrModule.Configuration,
    val clientSpiskammerset: ClientSpiskammersetModule.Configuration,
    val clientSpleis: ClientSpleisModule.Configuration,
    val db: DBModule.Configuration,
    val kafka: KafkaModule.Configuration,
    val environmentToggles: EnvironmentToggles,
    val stikkprøver: Stikkprøver,
    val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
    val tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
)
