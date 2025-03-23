package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.testApplication
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.oppgave.Egenskap
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
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture
import no.nav.helse.spesialist.test.TestPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

abstract class AbstractE2EIntegrationTest {
    private val kafkaModuleTestFixture = KafkaModuleTestRapidTestFixture()
    private val testPerson = TestPerson()
    private val testRapid = SimulatingTestRapid().also { rapid ->
        sequenceOf(
            AvviksvurderingbehovRiver(),
            HentPersoninfoV2behovRiver(testPerson),
            HentEnhetbehovRiver(testPerson),
            HentInfotrygdutbetalingerbehovRiver(testPerson),
            ArbeidsgiverinformasjonbehovRiver(testPerson),
            ArbeidsgiverinformasjonOgHentPersoninfoV2behovRiver(testPerson),
        ).forEach { it.registerOn(rapid) }
    }

    private val meldingssender = SimulatingTestRapidMeldingssender(testRapid)

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

    protected fun simulerPublisertSendtSøknadNavMelding() {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "sendt_søknad_nav",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "fnr" to testPerson.fødselsnummer,
                    "aktorId" to testPerson.aktørId,
                    "arbeidsgiver" to mapOf(
                        "orgnummer" to testPerson.orgnummer
                    )
                )
            ).toJson()
        )
    }

    protected fun simulerPublisertBehandlingOpprettetMelding(spleisBehandlingId: UUID) {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "behandling_opprettet",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
                    "behandlingId" to spleisBehandlingId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "aktørId" to testPerson.aktørId,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "fom" to (1 jan 2018),
                    "tom" to (31 jan 2018)
                )
            ).toJson()
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

    protected fun sendArbeidsforholdløsning() {
        meldingssender.sendArbeidsforholdløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1
        )
    }

    protected fun simulerPublisertGodkjenningsbehovMelding(spleisBehandlingId: UUID) {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "behov",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "@behov" to listOf("Godkjenning"),
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
                    "utbetalingId" to testPerson.utbetalingId1,
                    "Godkjenning" to mapOf(
                        "periodeFom" to (1 jan 2018),
                        "periodeTom" to (31 jan 2018),
                        "skjæringstidspunkt" to (1 jan 2018),
                        "periodetype" to "FØRSTEGANGSBEHANDLING",
                        "førstegangsbehandling" to true,
                        "utbetalingtype" to "UTBETALING",
                        "inntektskilde" to "EN_ARBEIDSGIVER",
                        "orgnummereMedRelevanteArbeidsforhold" to emptyList<String>(),
                        "kanAvvises" to true,
                        "vilkårsgrunnlagId" to UUID.randomUUID(),
                        "behandlingId" to spleisBehandlingId,
                        "tags" to emptyList<String>(),
                        "perioderMedSammeSkjæringstidspunkt" to listOf(
                            mapOf(
                                "fom" to (1 jan 2018),
                                "tom" to (31 jan 2018),
                                "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
                                "behandlingId" to spleisBehandlingId
                            )
                        ),
                        "sykepengegrunnlagsfakta" to mapOf(
                            "fastsatt" to "EtterHovedregel",
                            "arbeidsgivere" to listOf(
                                mapOf(
                                    "arbeidsgiver" to testPerson.orgnummer,
                                    "omregnetÅrsinntekt" to 123456.7,
                                    "inntektskilde" to "Arbeidsgiver",
                                )
                            )
                        ),
                        "omregnedeÅrsinntekter" to listOf(
                            mapOf(
                                "organisasjonsnummer" to testPerson.orgnummer,
                                "beløp" to 123456.7,
                            )
                        ),
                    ),
                )
            ).toJson()
        )
    }

    protected fun simulerPublisertVedtaksperiodeEndretMelding() {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "vedtaksperiode_endret",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "aktørId" to testPerson.aktørId,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "gjeldendeTilstand" to "AVVENTER_GODKJENNING",
                    "forrigeTilstand" to "AVVENTER_SIMULERING",
                    "@forårsaket_av" to mapOf(
                        "id" to UUID.randomUUID()
                    ),
                    "fom" to (1 jan 2018),
                    "tom" to (31 jan 2018)
                )
            ).toJson()
        )
    }

    protected fun simulerPublisertVedtaksperiodeNyUtbetalingMelding() {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "vedtaksperiode_ny_utbetaling",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to testPerson.vedtaksperiodeId1,
                    "utbetalingId" to testPerson.utbetalingId1,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "aktørId" to testPerson.aktørId,
                    "organisasjonsnummer" to testPerson.orgnummer,
                )
            ).toJson()
        )
    }

    protected fun simulerPublisertUtbetalingEndretMelding() {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "utbetaling_endret",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "utbetalingId" to testPerson.utbetalingId1,
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "type" to "UTBETALING",
                    "forrigeStatus" to "NY",
                    "gjeldendeStatus" to "IKKE_UTBETALT",
                    "@opprettet" to LocalDateTime.now(),
                    "arbeidsgiverOppdrag" to mapOf(
                        "mottaker" to testPerson.orgnummer,
                        "fagområde" to "SPREF",
                        "fagsystemId" to "LWCBIQLHLJISGREBICOHAU",
                        "nettoBeløp" to 20000,
                        "linjer" to listOf(
                            mapOf(
                                "fom" to LocalDate.now(),
                                "tom" to LocalDate.now(),
                                "totalbeløp" to 2000
                            ),
                            mapOf(
                                "fom" to LocalDate.now(),
                                "tom" to LocalDate.now(),
                                "totalbeløp" to 2000
                            )
                        )
                    ),
                    "personOppdrag" to mapOf(
                        "mottaker" to testPerson.fødselsnummer,
                        "fagområde" to "SP",
                        "fagsystemId" to "ASJKLD90283JKLHAS3JKLF",
                        "nettoBeløp" to 0,
                        "linjer" to listOf(
                            mapOf(
                                "fom" to LocalDate.now(),
                                "tom" to LocalDate.now(),
                                "totalbeløp" to 2000
                            ),
                            mapOf(
                                "fom" to LocalDate.now(),
                                "tom" to LocalDate.now(),
                                "totalbeløp" to 2000
                            )
                        )

                    )
                )
            ).toJson()
        )
    }

    protected fun simulerPublisertAktivitetsloggNyAktivitetMelding(varselkoder: List<String>) {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "aktivitetslogg_ny_aktivitet",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "aktiviteter" to varselkoder.map { varselkode ->
                        mapOf(
                            "id" to UUID.randomUUID(),
                            "melding" to "en melding",
                            "nivå" to "VARSEL",
                            "varselkode" to varselkode,
                            "tidsstempel" to LocalDateTime.now(),
                            "kontekster" to listOf(
                                mapOf(
                                    "konteksttype" to "Person",
                                    "kontekstmap" to mapOf(
                                        "fødselsnummer" to testPerson.fødselsnummer,
                                        "aktørId" to testPerson.aktørId
                                    )
                                ),
                                mapOf(
                                    "konteksttype" to "Arbeidsgiver",
                                    "kontekstmap" to mapOf(
                                        "organisasjonsnummer" to testPerson.orgnummer
                                    )
                                ),
                                mapOf(
                                    "konteksttype" to "Vedtaksperiode",
                                    "kontekstmap" to mapOf(
                                        "vedtaksperiodeId" to testPerson.vedtaksperiodeId1
                                    )
                                )
                            )
                        )
                    }
                )
            ).toJson()
        )
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
        val oppgaveId =
            modules.dbModule.daos.oppgaveDao.finnOppgaveId(testPerson.fødselsnummer)
                ?: error("Fant ikke oppgave for personen")
        val oppgave = modules.dbModule.sessionFactory.transactionalSessionScope { session ->
            session.oppgaveRepository.finn(oppgaveId) { _, _ -> true }
        } ?: error("Fant ikke oppgaven basert på ID")
        val egenskaper = oppgave.egenskaper
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toList())) { "Forventet å finne ${forventedeEgenskaper.toSet()} i $egenskaper" }
    }
}
