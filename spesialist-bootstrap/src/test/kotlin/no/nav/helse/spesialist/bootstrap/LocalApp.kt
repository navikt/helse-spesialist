package no.nav.helse.spesialist.bootstrap

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.application.tilgangskontroll.Gruppe
import no.nav.helse.spesialist.application.tilgangskontroll.Tilgangsgrupper
import no.nav.helse.spesialist.client.entraid.testfixtures.ClientEntraIDModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.krr.testfixtures.ClientKRRModuleIntegationTestFixture
import no.nav.helse.spesialist.client.spleis.testfixtures.ClientSpleisModuleIntegrationTestFixture
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleIntegrationTestFixture
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

fun main() {
    val rapidApp = RapidApp()
    val mockOAuth2Server = MockOAuth2Server().also { it.start() }
    val apiModuleIntegrationTestFixture = ApiModuleIntegrationTestFixture(mockOAuth2Server)
    rapidApp.start(
        configuration = Configuration(
            api = apiModuleIntegrationTestFixture.apiModuleConfiguration,
            clientEntraID = ClientEntraIDModuleIntegrationTestFixture(mockOAuth2Server).entraIDAccessTokenGeneratorConfiguration,
            clientKrr = ClientKRRModuleIntegationTestFixture.moduleConfiguration,
            clientSpleis = ClientSpleisModuleIntegrationTestFixture.moduleConfiguration,
            db = DBTestFixture.database.dbModuleConfiguration,
            kafka = KafkaModuleIntegrationTestFixture.moduleConfiguration,
            versjonAvKode = "versjon_1",
            tilgangsgrupper = object : Tilgangsgrupper {
                override val kode7GruppeId: UUID = UUID.randomUUID()
                override val beslutterGruppeId: UUID = UUID.randomUUID()
                override val skjermedePersonerGruppeId: UUID = UUID.randomUUID()
                override val stikkprøveGruppeId: UUID = UUID.randomUUID()

                override fun gruppeId(gruppe: Gruppe): UUID {
                    return when (gruppe) {
                        Gruppe.KODE7 -> kode7GruppeId
                        Gruppe.BESLUTTER -> beslutterGruppeId
                        Gruppe.SKJERMEDE -> skjermedePersonerGruppeId
                        Gruppe.STIKKPRØVE -> stikkprøveGruppeId
                    }
                }
            },
            environmentToggles = object : EnvironmentToggles {
                override val kanBeslutteEgneSaker = false
                override val kanGodkjenneUtenBesluttertilgang = false
            },
            stikkprøver = object : Stikkprøver {
                override fun utsFlereArbeidsgivereFørstegangsbehandling(): Boolean = false
                override fun utsFlereArbeidsgivereForlengelse(): Boolean = false
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
        },
    )
}
