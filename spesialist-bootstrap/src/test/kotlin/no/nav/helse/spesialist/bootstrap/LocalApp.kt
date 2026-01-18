package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.application.tilgangskontroll.randomTilgangsgruppeUuider
import no.nav.helse.spesialist.client.entraid.testfixtures.ClientEntraIDModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.krr.testfixtures.ClientKRRModuleIntegationTestFixture
import no.nav.helse.spesialist.client.spleis.testfixtures.ClientSpleisModuleIntegrationTestFixture
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleIntegrationTestFixture
import no.nav.security.mock.oauth2.MockOAuth2Server

fun main() {
    val rapidApp = RapidApp()
    val mockOAuth2Server = MockOAuth2Server().also { it.start() }
    val tilgangsgruppeUuider = randomTilgangsgruppeUuider()
    val apiModuleIntegrationTestFixture = ApiModuleIntegrationTestFixture(mockOAuth2Server, tilgangsgruppeUuider)
    rapidApp.start(
        configuration = Configuration(
            api = apiModuleIntegrationTestFixture.apiModuleConfiguration,
            clientEntraID = ClientEntraIDModuleIntegrationTestFixture(mockOAuth2Server).entraIDAccessTokenGeneratorConfiguration,
            clientKrr = ClientKRRModuleIntegationTestFixture.moduleConfiguration,
            clientSpleis = ClientSpleisModuleIntegrationTestFixture.moduleConfiguration,
            db = DBTestFixture.database.dbModuleConfiguration,
            kafka = KafkaModuleIntegrationTestFixture.moduleConfiguration,
            tilgangsgruppeUuider = tilgangsgruppeUuider,
            environmentToggles = object : EnvironmentToggles {
                override val kanBeslutteEgneSaker = false
                override val kanGodkjenneUtenBesluttertilgang = false
            },
            stikkprøver = object : Stikkprøver {
                override fun utsFlereArbeidsgivereFørstegangsbehandling(): Boolean = false
                override fun utsFlereArbeidsgivereForlengelse(): Boolean = false
                override fun selvstendigNæringsdrivendeForlengelse(): Boolean = false
                override fun utsEnArbeidsgiverFørstegangsbehandling(): Boolean = false
                override fun utsEnArbeidsgiverForlengelse(): Boolean = false
                override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling(): Boolean = false
                override fun fullRefusjonFlereArbeidsgivereForlengelse(): Boolean = false
                override fun fullRefusjonEnArbeidsgiver(): Boolean = false
            },
        ),
        rapidsConnection = KafkaModuleIntegrationTestFixture.createRapidApplication { ktorApplication ->
            rapidApp.ktorSetupCallback(ktorApplication)
            apiModuleIntegrationTestFixture.addAdditionalRoutings(ktorApplication)
            println(
                """
            
                OAuth2-token:
                ${apiModuleIntegrationTestFixture.token}
            
                """.trimIndent()
            )
        },
    )
}
