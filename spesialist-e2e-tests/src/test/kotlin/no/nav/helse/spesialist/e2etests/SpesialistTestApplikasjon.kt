package no.nav.helse.spesialist.e2etests

import io.ktor.server.application.Application
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver
import no.nav.helse.rapids_rivers.NaisEndpoints
import no.nav.helse.rapids_rivers.ktorApplication
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.application.tilgangskontroll.tilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.application.tilgangskontroll.tilgangsgrupperTilTilganger
import no.nav.helse.spesialist.bootstrap.Configuration
import no.nav.helse.spesialist.bootstrap.RapidApp
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
import no.nav.helse.spesialist.e2etests.behovløserstubs.BehovLøserStub
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture
import no.nav.helse.spesialist.valkey.ValkeyModule
import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.random.Random

/**
 * En så ekte som mulig Spesialist, der alle integrasjoner mot omverdenen er stubbet.
 * Brukes både av E2E-testene og av den lokale appen med testdata.
 */
open class SpesialistTestApplikasjon(
    val port: Int = Random.nextInt(10000, 20000),
    personPseudoIdModuleLabel: String = "e2e-tests",
    ventPåKtorServer: Boolean = false,
    ekstraKtorModul: Application.(SpesialistTestApplikasjon) -> Unit = {},
) {
    val testRapid = LoopbackTestRapid()
    val behovLøserStub = BehovLøserStub(testRapid).also { it.registerOn(testRapid) }
    val spleisStub =
        SpleisStub(testRapid, ClientSpleisModuleIntegrationTestFixture.wireMockServer).also {
            it.registerOn(testRapid)
        }

    private val mockOAuth2Server = MockOAuth2Server().also { it.start() }
    val tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller()
    val tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger()
    val apiModuleIntegrationTestFixture =
        ApiModuleIntegrationTestFixture(mockOAuth2Server, tilgangsgrupperTilTilganger, tilgangsgrupperTilBrukerroller)
    val personPseudoidIntegrationTestFixture = PersonPseudoIdTestFixture(personPseudoIdModuleLabel)
    private val rapidApp = RapidApp()
    private val modules =
        rapidApp.start(
            configuration =
                Configuration(
                    api = apiModuleIntegrationTestFixture.apiModuleConfiguration,
                    clientEntraID = ClientEntraIDModuleIntegrationTestFixture().moduleConfiguration,
                    clientKrr = ClientKRRModuleIntegationTestFixture.moduleConfiguration,
                    clientSparkelNorg = ClientSparkelNorgModuleIntegrationTestFixture.moduleConfiguration,
                    clientSparkelSykepengeperioder = ClientSparkelSykepengeperioderModuleIntegrationTestFixture.moduleConfiguration,
                    clientSpleis = ClientSpleisModuleIntegrationTestFixture.moduleConfiguration,
                    clientSpForsikring = ClientSpForsikringModuleIntegrationTestFixture.moduleConfiguration,
                    db = DBTestFixture.database.dbModuleConfiguration,
                    kafka = KafkaModuleTestRapidTestFixture.moduleConfiguration,
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
                    clientSpeed = ClientSpeedModuleIntegrationTestFixture.moduleConfiguration,
                    valkey = ValkeyModule.Configuration(valkey = null),
                    clientPersonPseudoId = personPseudoidIntegrationTestFixture.moduleConfiguration,
                    clientTilgangsmaskinen = ClientTilgangsmaskinenModuleIntegrationTestFixture.moduleConfiguration,
                ),
            rapidsConnection = testRapid,
        )

    val dbModule = modules.dbModule

    init {
        ktorApplication(
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            naisEndpoints = NaisEndpoints.Default,
            port = port,
            aliveCheck = { true },
            readyCheck = { true },
            preStopHook = { },
            cioConfiguration = { },
            modules =
                listOf {
                    rapidApp.ktorSetupCallback(this)
                    ekstraKtorModul(this@SpesialistTestApplikasjon)
                },
        ).also { it.start(wait = ventPåKtorServer) }
    }
}
