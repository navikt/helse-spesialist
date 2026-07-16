package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.application.tilgangskontroll.tilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.tilgangsgrupperTilTilganger
import no.nav.helse.spesialist.client.entraid.testfixtures.ClientEntraIDModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.krr.testfixtures.ClientKRRModuleIntegationTestFixture
import no.nav.helse.spesialist.client.personpseudoid.PersonPseudoIdTestFixture
import no.nav.helse.spesialist.client.sparkel.norg.testfixtures.ClientSparkelNorgModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.sparkel.sykepengeperioder.testfixtures.ClientSparkelSykepengeperioderModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.speed.testfixtures.ClientSpeedModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.spforsikring.testfixtures.ClientSpForsikringModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.spleis.testfixtures.ClientSpleisModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.tilgangsmaskinen.testfixtures.ClientTilgangsmaskinenModuleIntegrationTestFixture
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleIntegrationTestFixture
import no.nav.helse.spesialist.valkey.ValkeyModule
import no.nav.security.mock.oauth2.MockOAuth2Server

fun main() {
    val rapidApp = RapidApp()
    val mockOAuth2Server = MockOAuth2Server().also { it.start() }
    val tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller()
    val tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger()
    val apiModuleIntegrationTestFixture =
        ApiModuleIntegrationTestFixture(mockOAuth2Server, tilgangsgrupperTilTilganger, tilgangsgrupperTilBrukerroller)
    val personPseudoidIntegrationTestFixture = PersonPseudoIdTestFixture("local-app")
    rapidApp.start(
        configuration =
            Configuration(
                api = apiModuleIntegrationTestFixture.apiModuleConfiguration,
                clientEntraID = ClientEntraIDModuleIntegrationTestFixture().moduleConfiguration,
                clientKrr = ClientKRRModuleIntegationTestFixture.moduleConfiguration,
                clientSparkelNorg = ClientSparkelNorgModuleIntegrationTestFixture.moduleConfiguration,
                clientSparkelSykepengeperioder = ClientSparkelSykepengeperioderModuleIntegrationTestFixture.moduleConfiguration,
                clientSpleis = ClientSpleisModuleIntegrationTestFixture.moduleConfiguration,
                clientSpeed = ClientSpeedModuleIntegrationTestFixture.moduleConfiguration,
                clientSpForsikring = ClientSpForsikringModuleIntegrationTestFixture.moduleConfiguration,
                clientPersonPseudoId = personPseudoidIntegrationTestFixture.moduleConfiguration,
                clientTilgangsmaskinen = ClientTilgangsmaskinenModuleIntegrationTestFixture.moduleConfiguration,
                db = DBTestFixture.database.dbModuleConfiguration,
                kafka = KafkaModuleIntegrationTestFixture.moduleConfiguration,
                valkey = ValkeyModule.Configuration(valkey = null),
                environmentToggles =
                    object : EnvironmentToggles {
                        override val kanBeslutteEgneSaker = false
                        override val kanGodkjenneUtenBesluttertilgang = false
                        override val kanSeForsikring = false
                        override val devGcp = false
                    },
                stikkprøver =
                    object : Stikkprøver.Configuration {
                        override fun utsFlereArbeidsgivereFørstegangsbehandling(): Boolean = false

                        override fun utsFlereArbeidsgivereForlengelse(): Boolean = false

                        override fun selvstendigNæringsdrivendeForlengelse(): Boolean = false

                        override fun utsEnArbeidsgiverFørstegangsbehandling(): Boolean = false

                        override fun utsEnArbeidsgiverForlengelse(): Boolean = false

                        override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling(): Boolean = false

                        override fun fullRefusjonFlereArbeidsgivereForlengelse(): Boolean = false

                        override fun fullRefusjonEnArbeidsgiver(): Boolean = false
                    },
                tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
                tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
            ),
        rapidsConnection =
            KafkaModuleIntegrationTestFixture.createRapidApplication { ktorApplication ->
                rapidApp.ktorSetupCallback(ktorApplication)
                apiModuleIntegrationTestFixture.addAdditionalRoutings(ktorApplication)
                println(
                    """
                    
                    OAuth2-token:
                    ${apiModuleIntegrationTestFixture.token}
                    
                    """.trimIndent(),
                )
            },
    )
}
