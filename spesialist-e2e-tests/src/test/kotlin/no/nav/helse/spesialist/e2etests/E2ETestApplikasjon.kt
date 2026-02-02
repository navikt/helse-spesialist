package no.nav.helse.spesialist.e2etests

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.rapids_rivers.NaisEndpoints
import no.nav.helse.rapids_rivers.ktorApplication
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.application.tilgangskontroll.randomTilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.bootstrap.Configuration
import no.nav.helse.spesialist.bootstrap.RapidApp
import no.nav.helse.spesialist.client.entraid.testfixtures.ClientEntraIDModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.krr.testfixtures.ClientKRRModuleIntegationTestFixture
import no.nav.helse.spesialist.client.spleis.testfixtures.ClientSpleisModuleIntegrationTestFixture
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.e2etests.behovløserstubs.BehovLøserStub
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture
import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.random.Random

object E2ETestApplikasjon {
    val testRapid = LoopbackTestRapid()
    val behovLøserStub = BehovLøserStub(testRapid).also { it.registerOn(testRapid) }
    val spleisStub = SpleisStub(testRapid, ClientSpleisModuleIntegrationTestFixture.wireMockServer).also {
        it.registerOn(testRapid)
    }

    private val mockOAuth2Server = MockOAuth2Server().also { it.start() }
    val tilgangsgrupperTilBrukerroller = randomTilgangsgrupperTilBrukerroller()
    val apiModuleIntegrationTestFixture = ApiModuleIntegrationTestFixture(mockOAuth2Server, tilgangsgrupperTilBrukerroller)
    private val rapidApp = RapidApp()
    private val modules = rapidApp.start(
        configuration = Configuration(
            api = apiModuleIntegrationTestFixture.apiModuleConfiguration,
            clientEntraID = ClientEntraIDModuleIntegrationTestFixture(mockOAuth2Server).entraIDAccessTokenGeneratorConfiguration,
            clientKrr = ClientKRRModuleIntegationTestFixture.moduleConfiguration,
            clientSpleis = ClientSpleisModuleIntegrationTestFixture.moduleConfiguration,
            db = DBTestFixture.database.dbModuleConfiguration,
            kafka = KafkaModuleTestRapidTestFixture.moduleConfiguration,
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
            tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller
        ),
        rapidsConnection = testRapid,
    )

    val port = Random.nextInt(10000, 20000)

    init {
        ktorApplication(
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            naisEndpoints = NaisEndpoints.Default,
            port = port,
            aliveCheck = { true },
            readyCheck = { true },
            preStopHook = { },
            cioConfiguration = { },
            modules = listOf {
                rapidApp.ktorSetupCallback(this)
            }
        ).also { it.start() }
    }

    val dbModule = modules.dbModule
}
