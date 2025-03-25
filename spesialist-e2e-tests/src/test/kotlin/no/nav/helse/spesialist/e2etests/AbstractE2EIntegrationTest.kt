package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.testApplication
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
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
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.e2etests.behovløserstubs.ArbeidsforholdBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.ArbeidsgiverinformasjonBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.AvviksvurderingBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.BehovLøserStub
import no.nav.helse.spesialist.e2etests.behovløserstubs.EgenAnsattBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.FullmaktBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.HentEnhetBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.HentInfotrygdutbetalingerBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.HentPersoninfoV2BehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.InntekterForSykepengegrunnlagBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.RisikovurderingBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.VergemålBehovLøser
import no.nav.helse.spesialist.e2etests.behovløserstubs.ÅpneOppgaverBehovLøser
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture
import no.nav.helse.spesialist.test.TestPerson
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

abstract class AbstractE2EIntegrationTest {
    private val testPerson = TestPerson()
    protected val vedtaksperiodeId = UUID.randomUUID()
    protected val risikovurderingBehovLøser = RisikovurderingBehovLøser()
    protected val åpneOppgaverBehovLøser = ÅpneOppgaverBehovLøser()
    private val testRapid = LoopbackTestRapid()
    protected val behovLøserStub = BehovLøserStub(
        testRapid,
        ArbeidsforholdBehovLøser(),
        ArbeidsgiverinformasjonBehovLøser(),
        AvviksvurderingBehovLøser(),
        EgenAnsattBehovLøser(),
        HentEnhetBehovLøser(),
        HentInfotrygdutbetalingerBehovLøser(testPerson.orgnummer),
        HentPersoninfoV2BehovLøser(testPerson),
        InntekterForSykepengegrunnlagBehovLøser(testPerson.orgnummer),
        risikovurderingBehovLøser,
        FullmaktBehovLøser(),
        VergemålBehovLøser(),
        åpneOppgaverBehovLøser,
    ).also {
        it.registerOn(testRapid)
    }

    private val modules = RapidApp.start(
        configuration = Configuration(
            api = ApiModuleIntegrationTestFixture.apiModuleConfiguration,
            clientEntraID = ClientEntraIDModuleIntegrationTestFixture.entraIDAccessTokenGeneratorConfiguration,
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

    protected fun simulerPublisertBehandlingOpprettetMelding(
        spleisBehandlingId: UUID,
        vedtaksperiodeId: UUID = this.vedtaksperiodeId
    ) {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "behandling_opprettet",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to vedtaksperiodeId,
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

    protected fun simulerPublisertGodkjenningsbehovMelding(
        spleisBehandlingId: UUID,
        vedtaksperiodeId: UUID = this.vedtaksperiodeId
    ) {
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
                    "vedtaksperiodeId" to vedtaksperiodeId,
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
                                "vedtaksperiodeId" to vedtaksperiodeId,
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

    protected fun simulerPublisertGosysOppgaveEndretMelding() {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "gosys_oppgave_endret",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "fødselsnummer" to testPerson.fødselsnummer
                )
            ).toJson()
        )
    }

    protected fun simulerPublisertVedtaksperiodeEndretMelding(vedtaksperiodeId: UUID = this.vedtaksperiodeId) {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "vedtaksperiode_endret",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to vedtaksperiodeId,
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

    protected fun simulerPublisertVedtaksperiodeNyUtbetalingMelding(vedtaksperiodeId: UUID = this.vedtaksperiodeId) {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "vedtaksperiode_ny_utbetaling",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to vedtaksperiodeId,
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

    protected fun simulerPublisertAktivitetsloggNyAktivitetMelding(
        varselkoder: List<String>,
        vedtaksperiodeId: UUID = this.vedtaksperiodeId
    ) {
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
                                        "vedtaksperiodeId" to vedtaksperiodeId
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
        val oppgave = finnOppgave()
        val egenskaper = oppgave.egenskaper
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toList())) { "Forventet å finne ${forventedeEgenskaper.toSet()} i $egenskaper" }
    }

    private fun finnOppgave(): Oppgave {
        val oppgaveId =
            modules.dbModule.daos.oppgaveDao.finnOppgaveIdUansettStatus(testPerson.fødselsnummer)
        val oppgave = modules.dbModule.sessionFactory.transactionalSessionScope { session ->
            session.oppgaveRepository.finn(oppgaveId) { _, _ -> true }
        } ?: error("Fant ikke oppgaven basert på ID")
        return oppgave
    }

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

    protected fun simulerFremTilOgMedGodkjenningsbehov(): UUID {
        val spleisBehandlingId = simulerFremTilOgMedNyUtbetaling()
        simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(spleisBehandlingId)
        return spleisBehandlingId
    }

    protected fun simulerFremTilOgMedNyUtbetaling(vedtaksperiodeId: UUID = this.vedtaksperiodeId): UUID {
        simulerPublisertSendtSøknadNavMelding()
        val spleisBehandlingId = UUID.randomUUID()
        simulerPublisertBehandlingOpprettetMelding(
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = vedtaksperiodeId
        )
        simulerPublisertVedtaksperiodeNyUtbetalingMelding(vedtaksperiodeId = vedtaksperiodeId)
        return spleisBehandlingId
    }

    protected fun simulerFraNyUtbetalingTilOgMedGodkjenningsbehov(
        spleisBehandlingId: UUID,
        vedtaksperiodeId: UUID = this.vedtaksperiodeId
    ) {
        simulerPublisertUtbetalingEndretMelding()
        simulerPublisertVedtaksperiodeEndretMelding(vedtaksperiodeId = vedtaksperiodeId)
        simulerPublisertGodkjenningsbehovMelding(
            spleisBehandlingId = spleisBehandlingId,
            vedtaksperiodeId = vedtaksperiodeId
        )
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

    protected fun håndterUtbetalingUtbetalt() {
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
                    "forrigeStatus" to "SENDT",
                    "gjeldendeStatus" to "UTBETALT",
                    "@opprettet" to LocalDateTime.now(),
                    "arbeidsgiverOppdrag" to mapOf(
                        "mottaker" to testPerson.orgnummer,
                        "fagområde" to "SPREF",
                        "fagsystemId" to "LWCBIQLHLJISGREBICOHAU",
                        "nettoBeløp" to 20000,
                        "linjer" to listOf(
                            mapOf(
                                "fom" to "${LocalDate.now()}",
                                "tom" to "${LocalDate.now()}",
                                "totalbeløp" to 2000
                            ),
                            mapOf(
                                "fom" to "${LocalDate.now()}",
                                "tom" to "${LocalDate.now()}",
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
                                "fom" to "${LocalDate.now()}",
                                "tom" to "${LocalDate.now()}",
                                "totalbeløp" to 2000
                            ),
                            mapOf(
                                "fom" to "${LocalDate.now()}",
                                "tom" to "${LocalDate.now()}",
                                "totalbeløp" to 2000
                            )
                        )

                    )
                )
            ).toJson()
        )
    }

    protected fun håndterAvsluttetMedVedtak(spleisBehandlingId: UUID) {
        testRapid.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "avsluttet_med_vedtak",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "vedtaksperiodeId" to this.vedtaksperiodeId,
                    "behandlingId" to spleisBehandlingId,
                    "fom" to (1 jan 2018),
                    "tom" to (31 jan 2018),
                    "skjæringstidspunkt" to (1 jan 2018),
                    "sykepengegrunnlag" to 600000.0,
                    "grunnlagForSykepengegrunnlag" to 600000.0,
                    "grunnlagForSykepengegrunnlagPerArbeidsgiver" to emptyMap<String, Double>(),
                    "begrensning" to "VET_IKKE",
                    "inntekt" to 600000.0,
                    "vedtakFattetTidspunkt" to LocalDateTime.now(),
                    "hendelser" to emptyList<String>(),
                    "utbetalingId" to testPerson.utbetalingId1,
                    "sykepengegrunnlagsfakta" to mapOf(
                        "fastsatt" to "EtterHovedregel",
                        "omregnetÅrsinntekt" to 600000.0,
                        "6G" to 6 * 118620.0,
                        "arbeidsgivere" to listOf(
                            mapOf(
                                "arbeidsgiver" to testPerson.orgnummer,
                                "omregnetÅrsinntekt" to 600000.00,
                            )
                        ),
                        "innrapportertÅrsinntekt" to 600000.0,
                        "avviksprosent" to 0,
                    ),
                )
            ).toJson()
        )
    }
}
