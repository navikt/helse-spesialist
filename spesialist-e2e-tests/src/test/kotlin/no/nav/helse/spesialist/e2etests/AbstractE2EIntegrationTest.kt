package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.testApplication
import no.nav.helse.GodkjenningsbehovTestdata
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.vedtaksperiode.Inntektsopplysningkilde
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.SykepengegrunnlagsArbeidsgiver
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.testfixtures.ApiModuleIntegrationTestFixture
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandlerFraApi
import no.nav.helse.spesialist.bootstrap.Configuration
import no.nav.helse.spesialist.bootstrap.RapidApp
import no.nav.helse.spesialist.client.entraid.testfixtures.ClientEntraIDModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.krr.testfixtures.ClientKRRModuleIntegationTestFixture
import no.nav.helse.spesialist.client.spleis.testfixtures.ClientSpleisModuleIntegrationTestFixture
import no.nav.helse.spesialist.client.unleash.testfixtures.ClientUnleashModuleIntegrationTestFixture
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture
import no.nav.helse.spesialist.test.TestPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertNotNull

abstract class AbstractE2EIntegrationTest {
    private val kafkaModuleTestFixture = KafkaModuleTestRapidTestFixture()
    private val testRapid = SimulatingTestRapid().also { rapid ->
        AvviksvurderingbehovRiver().registerOn(rapid)
    }

    private val meldingssender = SimulatingTestRapidMeldingssender(testRapid)

    protected val testPerson = TestPerson()

    private val modules = RapidApp.start(
        configuration = Configuration(
            api = ApiModuleIntegrationTestFixture.apiModuleConfiguration,
            clientEntraID = ClientEntraIDModuleIntegrationTestFixture.entraIDAccessTokenGeneratorConfiguration,
            clientKrr = ClientKRRModuleIntegationTestFixture.moduleConfiguration,
            clientSpleis = ClientSpleisModuleIntegrationTestFixture.moduleConfiguration,
            clientUnleash = ClientUnleashModuleIntegrationTestFixture.moduleConfiguration,
            db = DBTestFixture.database.dbModuleConfiguration,
            kafka = kafkaModuleTestFixture.moduleConfiguration,
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

    private val dbQuery = DbQuery(modules.dbModule.dataSource)

    fun callGraphQL(
        @Language("GraphQL") query: String,
        saksbehandlerFraApi: SaksbehandlerFraApi = lagSaksbehandlerFraApi()
    ) {
        testApplication {
            application {
                RapidApp.ktorSetupCallback(this)
            }

            createClient {
                this.install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
            }.post("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(ApiModuleIntegrationTestFixture.token(saksbehandlerFraApi))
                setBody(mapOf("query" to query))
            }
        }
    }

    protected fun sendSøknadSendt() {
        meldingssender.sendSøknadSendt(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer
        )
    }

    protected fun sendBehandlingOpprettet(spleisBehandlingId: UUID) {
        meldingssender.sendBehandlingOpprettet(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            spleisBehandlingId = spleisBehandlingId,
        )
    }

    protected fun sendInntektløsning() {
        meldingssender.sendInntektløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            orgnr = testPerson.orgnummer
        )
    }

    protected fun sendRisikovurderingløsning() {
        meldingssender.sendRisikovurderingløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            kanGodkjennesAutomatisk = false,
            funn = emptyList(),
        )
    }

    protected fun sendÅpneGosysOppgaverløsning() {
        meldingssender.sendÅpneGosysOppgaverløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            antall = 0,
            oppslagFeilet = false
        )
    }

    protected fun sendVergemålOgFullmaktløsning() {
        meldingssender.sendVergemålOgFullmaktløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            vergemål = emptyList(),
            fremtidsfullmakter = emptyList(),
            fullmakter = emptyList(),
        )
    }

    protected fun sendEgenAnsattløsning() {
        meldingssender.sendEgenAnsattløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            erEgenAnsatt = false
        )
    }

    protected fun sendPersoninfoløsning() {
        meldingssender.sendPersoninfoløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            adressebeskyttelse = Adressebeskyttelse.Ugradert,
        )
    }

    protected fun sendEnhetløsning() {
        meldingssender.sendEnhetløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            enhet = "0301"
        )
    }

    protected fun sendInfotrygdutbetalingerløsning() {
        meldingssender.sendInfotrygdutbetalingerløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
        )
    }

    protected fun sendArbeidsgiverinformasjonløsning() {
        val erKompositt = testRapid.messageLog.last { it.path("@event_name").asText() == "behov" }
            .takeIf {
                it.path("@behov").map(JsonNode::asText).containsAll(
                    arrayOf(
                        "Arbeidsgiverinformasjon",
                        "HentPersoninfoV2"
                    ).toList()
                ) && !it.hasNonNull("@løsning")
            } != null
        if (erKompositt) {
            meldingssender.sendArbeidsgiverinformasjonløsningKompositt(
                aktørId = testPerson.aktørId,
                fødselsnummer = testPerson.aktørId,
                organisasjonsnummer = testPerson.orgnummer,
                vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            )
        } else {
            meldingssender.sendArbeidsgiverinformasjonløsning(
                aktørId = testPerson.aktørId,
                fødselsnummer = testPerson.aktørId,
                organisasjonsnummer = testPerson.orgnummer,
                vedtaksperiodeId = testPerson.vedtaksperiodeId1,
                arbeidsgiverinformasjonJson = null,
            )
        }
    }

    protected fun sendArbeidsforholdløsning(
    ) {
        meldingssender.sendArbeidsforholdløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1
        )
    }

    protected fun sendGodkjenningsbehov(spleisBehandlingId: UUID) =
        meldingssender.sendGodkjenningsbehov(
            GodkjenningsbehovTestdata(
                fødselsnummer = testPerson.fødselsnummer,
                aktørId = testPerson.aktørId,
                organisasjonsnummer = testPerson.orgnummer,
                vedtaksperiodeId = testPerson.vedtaksperiodeId1,
                utbetalingId = testPerson.utbetalingId1,
                spleisBehandlingId = spleisBehandlingId,
                spleisSykepengegrunnlagsfakta = SpleisSykepengegrunnlagsfakta(
                    arbeidsgivere = listOf(
                        SykepengegrunnlagsArbeidsgiver(
                            arbeidsgiver = testPerson.orgnummer,
                            omregnetÅrsinntekt = 123456.7,
                            inntektskilde = Inntektsopplysningkilde.Arbeidsgiver,
                            skjønnsfastsatt = null,
                        )
                    )
                ),
            )
        )

    protected fun sendVedtaksperiodeEndret() {
        meldingssender.sendVedtaksperiodeEndret(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            forrigeTilstand = "AVVENTER_SIMULERING",
            gjeldendeTilstand = "AVVENTER_GODKJENNING",
            forårsaketAvId = UUID.randomUUID(),
        )
    }

    protected fun sendVedtaksperiodeNyUtbetaling() {
        meldingssender.sendVedtaksperiodeNyUtbetaling(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            utbetalingId = testPerson.utbetalingId1,
        )
    }

    protected fun sendUtbetalingEndret() {
        meldingssender.sendUtbetalingEndret(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            utbetalingId = testPerson.utbetalingId1,
            type = "UTBETALING",
            arbeidsgiverbeløp = 20000,
            personbeløp = 0,
            forrigeStatus = NY,
            gjeldendeStatus = IKKE_UTBETALT,
            opprettet = LocalDateTime.now(),
        )
    }

    protected fun sendAktivitetsloggNyAktivitet(varselkoder: List<String>) {
        meldingssender.sendAktivitetsloggNyAktivitet(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            varselkoder = varselkoder,
        )
    }

    protected fun opprettVarseldefinisjoner(varselkoder: List<String>) {
        varselkoder.forEach {
            lagVarseldefinisjon(it)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        dbQuery.update(
            """
            INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet)
            VALUES (:unikId, :varselkode, :tittel, :forklaring, :handling, :avviklet, :opprettet)
            ON CONFLICT (unik_id) DO NOTHING
            """.trimIndent(),
            "unikId" to UUID.nameUUIDFromBytes(varselkode.toByteArray()),
            "varselkode" to varselkode,
            "tittel" to "En tittel for varselkode=$varselkode",
            "forklaring" to "En forklaring for varselkode=$varselkode",
            "handling" to "En handling for varselkode=$varselkode",
            "avviklet" to false,
            "opprettet" to LocalDateTime.now(),
        )
    }

    protected fun assertHarOppgaveegenskap(
        vararg forventedeEgenskaper: Egenskap,
    ) {
        val oppgaveId =
            modules.dbModule.daos.oppgaveDao.finnOppgaveId(testPerson.fødselsnummer)
                ?: error("Fant ikke oppgave for personen")
        val oppgave = modules.dbModule.sessionFactory.transactionalSessionScope { session ->
            session.oppgaveRepository.finn(oppgaveId) { _, _ -> true }
        } ?: error("Fant ikke oppgaven basert på ID")
        val egenskaper = oppgave.egenskaper
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toList())) { "Forventet å finne ${forventedeEgenskaper.toSet()} i $egenskaper" }
    }

    protected fun assertPersonEksisterer() {
        val minimalPerson = modules.dbModule.daos.personDao.finnMinimalPerson(testPerson.fødselsnummer)
        assertNotNull(minimalPerson)
        assertEquals(testPerson.fødselsnummer, minimalPerson.fødselsnummer)
        assertEquals(testPerson.aktørId, minimalPerson.aktørId)
    }

    protected fun assertArbeidsgiverEksisterer(organisasjonsnummer: String) {
        val arbeidsgiverEksisterer = dbQuery.single(
            "SELECT EXISTS(select 1 FROM arbeidsgiver WHERE organisasjonsnummer = :organisasjonsnummer)",
            "organisasjonsnummer" to organisasjonsnummer,
        ) { row -> row.boolean(1) }
        assertTrue(arbeidsgiverEksisterer)
    }

    protected fun assertVedtaksperiodeEksisterer(vedtaksperiodeId: UUID) {
        assertNotNull(modules.dbModule.daos.vedtakDao.finnVedtaksperiode(vedtaksperiodeId))
    }
}
