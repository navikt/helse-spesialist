package no.nav.helse.spesialist.e2etests

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.testApplication
import no.nav.helse.AvviksvurderingTestdata
import no.nav.helse.Meldingssender
import no.nav.helse.TestRapidHelpers.behov
import no.nav.helse.TestRapidHelpers.sisteBehov
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
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
import no.nav.helse.spesialist.kafka.testfixtures.KafkaModuleTestRapidTestFixture
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.util.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertNotNull

abstract class AbstractE2EIntegrationTest {
    private val kafkaModuleTestFixture = KafkaModuleTestRapidTestFixture()
    private val testRapid = kafkaModuleTestFixture.testRapid

    private val meldingssender = Meldingssender(kafkaModuleTestFixture.testRapid)

    private val avviksvurderingTestdata = AvviksvurderingTestdata()
    private val testPerson = TestPerson()

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

    protected fun vedtaksløsningenMottarNySøknad(testperson: TestPerson) {
        meldingssender.sendSøknadSendt(
            aktørId = testperson.aktørId,
            fødselsnummer = testperson.fødselsnummer,
            organisasjonsnummer = testperson.orgnummer
        )
        assertIngenEtterspurteBehov()
        assertPersonEksisterer(testperson)
    }

    protected fun spleisOppretterNyBehandling(
        testPerson: TestPerson,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        spleisBehandlingId: UUID = UUID.randomUUID(),
    ) {
        meldingssender.sendBehandlingOpprettet(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            fom = fom,
            tom = tom,
            spleisBehandlingId = spleisBehandlingId,
        )
        assertIngenEtterspurteBehov()
        assertArbeidsgiverEksisterer(testPerson.orgnummer)
        assertVedtaksperiodeEksisterer(testPerson.vedtaksperiodeId1)
    }

    protected fun spesialistBehandlerGodkjenningsbehovFremTilOppgave(regelverksvarsler: List<String>) {
        spesialistBehandlerGodkjenningsbehovFremTilRisikovurdering(regelverksvarsler = regelverksvarsler)
        håndterRisikovurderingløsning()
        håndterInntektløsning()
    }

    protected fun håndterInntektløsning() {
        assertEtterspurteBehov("InntekterForSykepengegrunnlag")
        meldingssender.sendInntektløsning(testPerson.aktørId, testPerson.fødselsnummer, testPerson.orgnummer)
    }

    protected fun håndterRisikovurderingløsning() {
        assertEtterspurteBehov("Risikovurdering")
        meldingssender.sendRisikovurderingløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            kanGodkjennesAutomatisk = false,
            funn = emptyList(),
        )
    }

    private fun spesialistBehandlerGodkjenningsbehovFremTilRisikovurdering(
        regelverksvarsler: List<String>,
    ) {
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(regelverksvarsler = regelverksvarsler)
        håndterÅpneOppgaverløsning()
    }

    protected fun håndterÅpneOppgaverløsning() {
        assertEtterspurteBehov("ÅpneOppgaver")
        meldingssender.sendÅpneGosysOppgaverløsning(testPerson.aktørId, testPerson.fødselsnummer, 0, false)
    }

    protected fun spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(regelverksvarsler: List<String>) {
        spesialistBehandlerGodkjenningsbehovFremTilVergemål(
            regelverksvarsler = regelverksvarsler,
        )
        håndterVergemålOgFullmaktløsning()
    }

    protected fun håndterVergemålOgFullmaktløsning(
    ) {
        assertEtterspurteBehov("Vergemål", "Fullmakt")
        meldingssender.sendVergemålOgFullmaktløsning(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            vergemål = emptyList(),
            fremtidsfullmakter = emptyList(),
            fullmakter = emptyList(),
        )
    }

    protected fun spesialistBehandlerGodkjenningsbehovFremTilVergemål(
        regelverksvarsler: List<String>
    ) {
        spesialistBehandlerGodkjenningsbehovFremTilEgenAnsatt(regelverksvarsler = regelverksvarsler)

        håndterEgenansattløsning(fødselsnummer = testPerson.fødselsnummer)
    }

    protected fun håndterEgenansattløsning() {
        assertEtterspurteBehov("EgenAnsatt")
        meldingssender.sendEgenAnsattløsning(
            testPerson.aktørId,
            testPerson.fødselsnummer,
            false
        )
    }

    protected fun spesialistBehandlerGodkjenningsbehovFremTilEgenAnsatt(
        regelverksvarsler: List<String> = emptyList(),
    ) {
        if (regelverksvarsler.isNotEmpty()) håndterAktivitetsloggNyAktivitet(varselkoder = regelverksvarsler)
        håndterGodkjenningsbehov()

        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        håndterArbeidsforholdløsning()
    }

    protected fun håndterPersoninfoløsning() {
        assertEtterspurteBehov("HentPersoninfoV2")
        meldingssender.sendPersoninfoløsning(
            testPerson.aktørId,
            testPerson.fødselsnummer,
            Adressebeskyttelse.Ugradert,
        )
    }

    protected fun håndterEnhetløsning() {
        assertEtterspurteBehov("HentEnhet")
        meldingssender.sendEnhetløsning(
            testPerson.aktørId,
            testPerson.fødselsnummer,
            testPerson.orgnummer,
            testPerson.vedtaksperiodeId1,
            "0301"
        )
    }

    protected fun håndterInfotrygdutbetalingerløsning() {
        val aktørId: String = testPerson.aktørId
        val fødselsnummer: String = testPerson.fødselsnummer
        val organisasjonsnummer: String = testPerson.orgnummer
        val vedtaksperiodeId: UUID = testPerson.vedtaksperiodeId1
        assertEtterspurteBehov("HentInfotrygdutbetalinger")
        meldingssender.sendInfotrygdutbetalingerløsning(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
        )
    }

    protected fun håndterArbeidsgiverinformasjonløsning() {
        val erKompositt = testRapid.inspektør.sisteBehov("Arbeidsgiverinformasjon", "HentPersoninfoV2") != null
        if (erKompositt) {
            assertEtterspurteBehov("Arbeidsgiverinformasjon", "HentPersoninfoV2")
            meldingssender.sendArbeidsgiverinformasjonKompositt(
                testPerson.aktørId,
                testPerson.aktørId,
                testPerson.orgnummer,
                testPerson.vedtaksperiodeId1,
            )
            return
        }
        assertEtterspurteBehov("Arbeidsgiverinformasjon")
        meldingssender.sendArbeidsgiverinformasjonløsning(
            testPerson.aktørId,
            testPerson.aktørId,
            testPerson.orgnummer,
            testPerson.vedtaksperiodeId1,
            null as List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson>?,
        )
    }

    protected fun håndterArbeidsforholdløsning(
    ) {
        assertEtterspurteBehov("Arbeidsforhold")
        meldingssender.sendArbeidsforholdløsning(
            testPerson.aktørId,
            testPerson.fødselsnummer,
            testPerson.orgnummer,
            testPerson.vedtaksperiodeId1
        )
    }

    protected fun håndterGodkjenningsbehov() {
        håndterGodkjenningsbehovUtenValidering()

        håndterAvviksvurderingløsning()

        assertEtterspurteBehov("HentPersoninfoV2")
    }

    private fun håndterAvviksvurderingløsning() {
        assertEtterspurteBehov("Avviksvurdering")
        meldingssender.sendAvviksvurderingløsning(
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            sammenligningsgrunnlagTotalbeløp = avviksvurderingTestdata.sammenligningsgrunnlag,
            avviksprosent = avviksvurderingTestdata.avviksprosent,
            avviksvurderingId = avviksvurderingTestdata.avviksvurderingId
        )
    }

    protected fun håndterGodkjenningsbehovUtenValidering() {
        val erRevurdering = erRevurdering(testPerson.vedtaksperiodeId1)
        håndterVedtaksperiodeNyUtbetaling()
        håndterUtbetalingOpprettet(
            fødselsnummer = testPerson.fødselsnummer,
            utbetalingtype = if (erRevurdering) "REVURDERING" else "UTBETALING",
            utbetalingId = testPerson.utbetalingId1,
            arbeidsgiverbeløp = 20000,
            personbeløp = 0,
        )
        håndterVedtaksperiodeEndret(
            fødselsnummer = testPerson.fødselsnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1
        )
        sisteMeldingId = sendGodkjenningsbehov(testPerson)
        sisteGodkjenningsbehovId = sisteMeldingId
    }

    protected fun håndterVedtaksperiodeNyUtbetaling() {
        meldingssender.sendVedtaksperiodeNyUtbetaling(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            utbetalingId = testPerson.utbetalingId1,
        )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterUtbetalingOpprettet(
    ) {
        val aktørId: String = testPerson.aktørId
        val fødselsnummer: String = testPerson.fødselsnummer
        val organisasjonsnummer: String = testPerson.orgnummer
        val utbetalingtype: String = "UTBETALING"
        val arbeidsgiverbeløp: Int = 20000
        val personbeløp: Int = 0
        val utbetalingId: UUID = testPerson.utbetalingId1
        håndterUtbetalingEndret(
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            utbetalingtype,
            arbeidsgiverbeløp,
            personbeløp,
            utbetalingId = utbetalingId,
        )
        assertIngenEtterspurteBehov()
    }

    protected fun håndterUtbetalingEndret(
    ) {
        val aktørId: String = testperson.aktørId
        val fødselsnummer: String = FØDSELSNUMMER
        val organisasjonsnummer: String = ORGNR
        val utbetalingtype: String = "UTBETALING"
        val arbeidsgiverbeløp: Int = 20000
        val personbeløp: Int = 0
        val forrigeStatus: Utbetalingsstatus = NY
        val gjeldendeStatus: Utbetalingsstatus = IKKE_UTBETALT
        val opprettet: LocalDateTime = LocalDateTime.now()
        val utbetalingId: UUID = utbetalingId
        sisteMeldingId =
            meldingssender.sendUtbetalingEndret(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = this.utbetalingId,
                type = utbetalingtype,
                arbeidsgiverbeløp = arbeidsgiverbeløp,
                personbeløp = personbeløp,
                forrigeStatus = forrigeStatus,
                gjeldendeStatus = gjeldendeStatus,
                opprettet = opprettet,
            )
    }

    protected fun håndterAktivitetsloggNyAktivitet(
        varselkoder: List<String> = emptyList(),
    ) {
        varselkoder.forEach {
            lagVarseldefinisjon(it)
        }
        meldingssender.sendAktivitetsloggNyAktivitet(
            aktørId = testPerson.aktørId,
            fødselsnummer = testPerson.fødselsnummer,
            organisasjonsnummer = testPerson.orgnummer,
            vedtaksperiodeId = testPerson.vedtaksperiodeId1,
            varselkoder = varselkoder,
        )
        assertIngenEtterspurteBehov()
    }

    protected fun assertIngenEtterspurteBehov() {
        assertEquals(emptyList<String>(), testRapid.inspektør.behov())
    }

    private fun assertEtterspurteBehov(vararg behov: String) {
        val etterspurteBehov = testRapid.inspektør.behov()
        assertEquals(behov.toList(), etterspurteBehov) {
            val ikkeEtterspurt = behov.toSet() - etterspurteBehov.toSet()
            "Forventet at følgende behov skulle være etterspurt: $ikkeEtterspurt\nFaktisk etterspurte behov: $etterspurteBehov\n"
        }
    }

    protected fun assertHarOppgaveegenskap(
        vararg forventedeEgenskaper: Egenskap,
    ) {
        val egenskaper = hentOppgaveegenskaper(
            modules.dbModule.daos.oppgaveDao.finnOppgaveId(testPerson.fødselsnummer) ?: error("Finner ikke oppgave")
        )
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toList())) { "Forventet å finne ${forventedeEgenskaper.toSet()} i $egenskaper" }
    }

    protected fun assertPersonEksisterer(testPerson: TestPerson) {
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

    private fun hentOppgaveegenskaper(oppgaveId: Long): Set<Egenskap> =
        modules.dbModule.sessionFactory.transactionalSessionScope { session -> session.oppgaveRepository.finn(oppgaveId) { _, _ -> true } }?.egenskaper
            ?: error("Fant ikke oppgave")

    private fun erRevurdering(vedtaksperiodeId: UUID) =
        modules.dbModule.daos.generasjonDao.finnGenerasjoner(vedtaksperiodeId)

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
}