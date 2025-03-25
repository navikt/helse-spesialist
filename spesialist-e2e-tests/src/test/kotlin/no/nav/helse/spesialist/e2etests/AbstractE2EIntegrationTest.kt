package no.nav.helse.spesialist.e2etests

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
    protected val spleisStub = SpleisStub(testPerson, testRapid)

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

    protected fun simulerFremTilOgMedGodkjenningsbehov() {
        spleisStub.simulerFremTilOgMedGodkjenningsbehov(vedtaksperiodeId)
    }

    protected fun simulerFremTilOgMedNyUtbetaling(vedtaksperiodeId: UUID = this.vedtaksperiodeId) {
        spleisStub.simulerFremTilOgMedNyUtbetaling(vedtaksperiodeId)
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

    protected fun håndterUtbetalingUtbetalt(vedtaksperiodeId: UUID = this.vedtaksperiodeId) {
        spleisStub.håndterUtbetalingUtbetalt(vedtaksperiodeId)
    }

    protected fun håndterAvsluttetMedVedtak(vedtaksperiodeId: UUID = this.vedtaksperiodeId) {
        spleisStub.håndterAvsluttetMedVedtak(vedtaksperiodeId)
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
}
