package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.rapids_rivers.NaisEndpoints
import no.nav.helse.rapids_rivers.ktorApplication
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandlerFraApi
import no.nav.helse.spesialist.bootstrap.Configuration
import no.nav.helse.spesialist.bootstrap.RapidApp
import no.nav.helse.spesialist.client.entraid.testfixtures.ClientEntraIDModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.krr.testfixtures.ClientKRRModuleIntegationTestFixture
import no.nav.helse.spesialist.client.spleis.testfixtures.ClientSpleisModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.unleash.testfixtures.ClientUnleashModuleIntegrationTestFixture
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.e2etests.behovløserstubs.BehovLøserStub
import no.nav.helse.spesialist.e2etests.behovløserstubs.RisikovurderingBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.ÅpneOppgaverBehovLøser
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture
import no.nav.helse.spesialist.test.TestPerson
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

abstract class AbstractE2EIntegrationTest {
    private val testPerson = TestPerson()
    protected val saksbehandler = lagSaksbehandlerFraApi()
    protected val vedtaksperiodeId = UUID.randomUUID()

    companion object {
        private val testRapid = LoopbackTestRapid()
        private val behovLøserStub = BehovLøserStub(testRapid).also { it.registerOn(testRapid) }
        private val spleisStub = SpleisStub(testRapid, ClientSpleisModuleIntegrationTestFixture.wireMockServer).also {
            it.registerOn(testRapid)
        }
        private val rapidApp = RapidApp()
        private val mockOAuth2Server = MockOAuth2Server().also { it.start() }
        private val apiModuleIntegrationTestFixture = ApiModuleIntegrationTestFixture(mockOAuth2Server)
        private val modules = rapidApp.start(
            configuration = Configuration(
                api = apiModuleIntegrationTestFixture.apiModuleConfiguration,
                clientEntraID = ClientEntraIDModuleIntegrationTestFixture(mockOAuth2Server).entraIDAccessTokenGeneratorConfiguration,
                clientKrr = ClientKRRModuleIntegationTestFixture.moduleConfiguration,
                clientSpleis = ClientSpleisModuleIntegrationTestFixture.moduleConfiguration,
                clientUnleash = ClientUnleashModuleIntegrationTestFixture.moduleConfiguration,
                db = DBTestFixture.database.dbModuleConfiguration,
                kafka = KafkaModuleTestRapidTestFixture.moduleConfiguration,
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
            rapidsConnection = testRapid,
        )
        val port = Random.nextInt(10000, 20000)
        private val ktorApp = ktorApplication(
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
        )
        private val httpClient: HttpClient =
            HttpClient(Apache) {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter())
                }
                engine {
                    socketTimeout = 5_000
                    connectTimeout = 5_000
                    connectionRequestTimeout = 5_000
                }
            }
    }

    init {
        behovLøserStub.init(testPerson)
        spleisStub.init(testPerson, vedtaksperiodeId)
        ktorApp.start()
    }

    protected val risikovurderingBehovLøser =
        behovLøserStub.finnLøser<RisikovurderingBehovLøser>(testPerson.fødselsnummer)
    protected val åpneOppgaverBehovLøser = behovLøserStub.finnLøser<ÅpneOppgaverBehovLøser>(testPerson.fødselsnummer)

    protected fun besvarBehovIgjen(behov: String) {
        behovLøserStub.besvarIgjen(testPerson.fødselsnummer, behov)
    }

    protected fun callGraphQL(operationName: String, variables: Map<String, Any>) = runBlocking {
        httpClient.post("http://localhost:$port/graphql") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(apiModuleIntegrationTestFixture.token(saksbehandler))
            setBody(
                mapOf(
                    "query" to (this::class.java.getResourceAsStream("/graphql/$operationName.graphql")
                        ?.use { it.reader().readText() }
                        ?: error("Fant ikke $operationName.graphql")),
                    "operationName" to operationName,
                    "variables" to variables))
        }.bodyAsText()
    }

    protected fun simulerFremTilOgMedGodkjenningsbehov() {
        spleisStub.simulerFremTilOgMedGodkjenningsbehov(testPerson, vedtaksperiodeId)
    }

    protected fun simulerFremTilOgMedNyUtbetaling(vedtaksperiodeId: UUID = this.vedtaksperiodeId) {
        spleisStub.simulerFremTilOgMedNyUtbetaling(testPerson, vedtaksperiodeId)
    }

    protected fun simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(vedtaksperiodeId: UUID = this.vedtaksperiodeId) {
        spleisStub.simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(vedtaksperiodeId)
    }

    protected fun simulerPublisertAktivitetsloggNyAktivitetMelding(
        varselkoder: List<String>,
        vedtaksperiodeId: UUID = this.vedtaksperiodeId
    ) {
        spleisStub.simulerPublisertAktivitetsloggNyAktivitetMelding(varselkoder, vedtaksperiodeId)
    }

    protected fun simulerPublisertGosysOppgaveEndretMelding(vedtaksperiodeId: UUID = this.vedtaksperiodeId) {
        spleisStub.simulerPublisertGosysOppgaveEndretMelding(vedtaksperiodeId)
    }

    protected fun lagreVarseldefinisjon(varselkode: String) {
        modules.dbModule.daos.definisjonDao.lagreDefinisjon(
            unikId = UUID.nameUUIDFromBytes(varselkode.toByteArray()),
            kode = varselkode,
            tittel = "En tittel for varselkode=$varselkode",
            forklaring = "En forklaring for varselkode=$varselkode",
            handling = "En handling for varselkode=$varselkode",
            avviklet = false,
            opprettet = LocalDateTime.now(),
        )
    }

    protected fun assertHarOppgaveegenskap(vararg forventedeEgenskaper: Egenskap) {
        val oppgave = finnOppgave()
        val egenskaper = oppgave.egenskaper
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toList())) { "Forventet å finne ${forventedeEgenskaper.toSet()} i $egenskaper" }
    }

    private fun finnOppgave(): Oppgave {
        val oppgave = modules.dbModule.sessionFactory.transactionalSessionScope { session ->
            session.oppgaveRepository.finn(finnOppgaveId()) { _, _ -> true }
        } ?: error("Fant ikke oppgaven basert på ID")
        return oppgave
    }

    protected fun finnOppgaveId() =
        modules.dbModule.daos.oppgaveDao.finnOppgaveIdUansettStatus(testPerson.fødselsnummer)

    protected fun finnGenerasjonId() = modules.dbModule.daos.oppgaveDao.finnGenerasjonId(finnOppgaveId())

    data class Varsel(
        val kode: String,
        val status: String,
    )

    protected fun hentVarselkoder(vedtaksperiodeId: UUID = this.vedtaksperiodeId): Set<Varsel> =
        sessionOf(modules.dbModule.dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT kode, status FROM selve_varsel WHERE vedtaksperiode_id = :vedtaksperiode_id"
            val paramMap = mapOf("vedtaksperiode_id" to vedtaksperiodeId)
            session.list(queryOf(query, paramMap)) { Varsel(it.string("kode"), it.string("status")) }.toSet()
        }

    protected fun assertOppgaveForPersonInvalidert() {
        assertEquals(Oppgave.Invalidert::class.java, finnOppgave().tilstand::class.java)
    }

    protected fun assertGodkjenningsbehovBesvart(
        godkjent: Boolean,
        automatiskBehandlet: Boolean,
        vararg årsakerTilAvvist: String,
    ) {
        val løsning = testRapid.meldingslogg
            .mapNotNull { it["@løsning"] }
            .mapNotNull { it["Godkjenning"] }
            .last()

        assertTrue(løsning["godkjent"].isBoolean)
        assertEquals(godkjent, løsning["godkjent"].booleanValue())
        assertEquals(automatiskBehandlet, løsning["automatiskBehandling"].booleanValue())
        assertNotNull(løsning["godkjenttidspunkt"].asLocalDateTime())
        if (årsakerTilAvvist.isNotEmpty()) {
            val begrunnelser = løsning["begrunnelser"].map { it.asText() }
            assertEquals(begrunnelser, begrunnelser.distinct())
            assertEquals(årsakerTilAvvist.toSet(), begrunnelser.toSet())
        }
    }

    protected fun assertBehandlingTilstand(expectedTilstand: String) {
        val actualTilstand = sessionOf(modules.dbModule.dataSource, strict = true).use { session ->
            session.run(
                asSQL(
                    "SELECT tilstand FROM behandling WHERE vedtaksperiode_id = :vedtaksperiode_id",
                    "vedtaksperiode_id" to vedtaksperiodeId,
                ).map { it.string("tilstand") }.asSingle
            )
        }
        assertEquals(expectedTilstand, actualTilstand)
    }

    protected fun saksbehandlerGodkjennerRisikovurderingVarsel() {
        val response = callGraphQL(
            operationName = "FetchPerson",
            variables = mapOf(
                "aktorId" to testPerson.aktørId,
            )
        )
        println(response)
        callGraphQL(
            operationName = "SettVarselStatus",
            variables = mapOf(
                "generasjonIdString" to finnGenerasjonId(),
                "varselkode" to "SB_RV_1",
                "ident" to saksbehandler.ident,
                "definisjonIdString" to "77970f04-c4c5-4b9f-8795-bb5e4749344c", // id fra api varseldefinisjon
            )
        )
    }

    protected fun saksbehandlerTildelerSegSaken() {
        callGraphQL(
            operationName = "Tildeling",
            variables = mapOf(
                "oppgavereferanse" to finnOppgaveId().toString(),
            )
        )
    }

    protected fun saksbehandlerFatterVedtak() {
        callGraphQL(
            operationName = "FattVedtak",
            variables = mapOf(
                "oppgavereferanse" to finnOppgaveId().toString(),
                "begrunnelse" to "Fattet vedtak",
            )
        )
    }
}
