import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.*
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.mediator.api.VedtaksperiodeMediator
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.modell.tildeling.TildelingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractE2ETest {
    protected companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val AKTØR = "999999999"
        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
        private val postgresPath = createTempDir()
        private val embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath)
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        private val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres").also(::println)
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }
        internal val dataSource = HikariDataSource(hikariConfig)

    }

    private val oppgaveDao = OppgaveDao(dataSource)
    private val personDao = PersonDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val warningDao = WarningDao(dataSource)
    private val commandContextDao = CommandContextDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val digitalKontaktinformasjonDao = DigitalKontaktinformasjonDao(dataSource)
    private val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val automatiseringDao = AutomatiseringDao(dataSource)
    private val hendelseDao = HendelseDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val egenAnsattDao = EgenAnsattDao(dataSource)
    private val utbetalingDao = UtbetalingDao(dataSource)

    protected val testRapid = TestRapid()

    private val meldingsfabrikk = Testmeldingfabrikk(UNG_PERSON_FNR_2018, AKTØR)

    protected val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)

    internal val miljøstyrtFeatureToggle = mockk<MiljøstyrtFeatureToggle> {
        every { risikovurdering() }.returns(false)
        every { automatisering() }.returns(false)
    }

    private val oppgaveMediator = OppgaveMediator(oppgaveDao, vedtakDao, tildelingDao)
    private val hendelsefabrikk = Hendelsefabrikk(
        hendelseDao = hendelseDao,
        personDao = personDao,
        arbeidsgiverDao = ArbeidsgiverDao(dataSource),
        vedtakDao = vedtakDao,
        warningDao = warningDao,
        oppgaveDao = oppgaveDao,
        commandContextDao = commandContextDao,
        snapshotDao = SnapshotDao(dataSource),
        reservasjonDao = ReservasjonDao(dataSource),
        saksbehandlerDao = SaksbehandlerDao(dataSource),
        overstyringDao = OverstyringDao(dataSource),
        risikovurderingDao = risikovurderingDao,
        digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
        åpneGosysOppgaverDao = åpneGosysOppgaverDao,
        egenAnsattDao = egenAnsattDao,
        speilSnapshotRestClient = restClient,
        oppgaveMediator = oppgaveMediator,
        miljøstyrtFeatureToggle = miljøstyrtFeatureToggle,
        automatisering = Automatisering(
            warningDao = warningDao,
            risikovurderingDao = risikovurderingDao,
            automatiseringDao = automatiseringDao,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle,
            personDao = personDao
        ) { false },
        utbetalingDao = utbetalingDao,
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao)
    )
    private val hendelseMediator = HendelseMediator(
        rapidsConnection = testRapid,
        oppgaveDao = oppgaveDao,
        vedtakDao = vedtakDao,
        personDao = personDao,
        commandContextDao = commandContextDao,
        hendelseDao = hendelseDao,
        hendelsefabrikk = hendelsefabrikk,
        oppgaveMediator = oppgaveMediator,
        tildelingDao = tildelingDao,
    )
    internal val vedtaksperiodeMediator = VedtaksperiodeMediator(
        vedtakDao = vedtakDao,
        warningDao = warningDao,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        snapshotDao = snapshotDao,
        overstyringDao = overstyringDao,
        oppgaveDao = oppgaveDao,
        tildelingDao = tildelingDao,
        risikovurderingDao = risikovurderingDao
    )

    @BeforeEach
    internal fun resetDatabase() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
            .load()
            .also {
                it.clean()
                it.migrate()
            }
    }

    @BeforeEach
    internal fun resetTestSetup() {
        clearMocks(restClient)
        testRapid.reset()
    }

    private fun nyHendelseId() = UUID.randomUUID()

    protected fun sendVedtaksperiodeForkastet(orgnr: String, vedtaksperiodeId: UUID) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet(id, vedtaksperiodeId, orgnr))
    }

    protected fun sendVedtaksperiodeEndret(orgnr: String, vedtaksperiodeId: UUID) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeEndret(id, vedtaksperiodeId, orgnr))
    }

    protected fun sendGodkjenningsbehov(
        orgnr: String,
        vedtaksperiodeId: UUID,
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        warnings: List<String> = emptyList(),
        periodetype: Saksbehandleroppgavetype = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
    ) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagGodkjenningsbehov(
                id,
                vedtaksperiodeId,
                orgnr,
                periodeFom,
                periodeTom,
                warnings,
                periodetype
            )
        )
    }

    protected fun sendPersoninfoløsning(
        hendelseId: UUID,
        orgnr: String,
        vedtaksperiodeId: UUID,
        contextId: UUID = testRapid.inspektør.contextId(),
        enhet: String = "0301"
    ) =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagPersoninfoløsning(
                    id,
                    hendelseId,
                    contextId,
                    vedtaksperiodeId,
                    orgnr,
                    enhet
                )
            )
        }

    protected fun sendOverstyrteDager(orgnr: String, saksbehandlerEpost: String, dager: List<OverstyringDagDto>) =
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagOverstyring(
                    id = id,
                    dager = dager,
                    organisasjonsnummer = orgnr,
                    saksbehandlerEpost = saksbehandlerEpost
                )
            )
        }

    protected fun sendDigitalKontaktinformasjonløsning(
        godkjenningsmeldingId: UUID,
        erDigital: Boolean = true
    ) {
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagDigitalKontaktinformasjonløsning(
                    id,
                    godkjenningsmeldingId,
                    testRapid.inspektør.contextId(),
                    erDigital
                )
            )
        }
    }

    protected fun sendÅpneGosysOppgaverløsning(
        godkjenningsmeldingId: UUID,
        antall: Int = 0,
        oppslagFeilet: Boolean = false
    ) {
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagÅpneGosysOppgaverløsning(
                    id,
                    godkjenningsmeldingId,
                    testRapid.inspektør.contextId(),
                    antall,
                    oppslagFeilet
                )
            )
        }
    }

    protected fun sendRisikovurderingløsning(
        godkjenningsmeldingId: UUID,
        vedtaksperiodeId: UUID,
        begrunnelser: List<String> = emptyList()
    ) {
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagRisikovurderingløsning(
                    id,
                    godkjenningsmeldingId,
                    testRapid.inspektør.contextId(),
                    vedtaksperiodeId,
                    begrunnelser
                )
            )
        }
    }

    protected fun sendEgenAnsattløsning(
        godkjenningsmeldingId: UUID,
        erEgenAnsatt: Boolean
    ) {
        nyHendelseId().also { id ->
            testRapid.sendTestMessage(
                meldingsfabrikk.lagEgenAnsattløsning(
                    id,
                    godkjenningsmeldingId,
                    testRapid.inspektør.contextId(),
                    erEgenAnsatt
                )
            )
        }
    }

    protected fun sendSaksbehandlerløsning(
        oppgaveId: Long,
        saksbehandlerIdent: String,
        saksbehandlerEpost: String,
        saksbehandlerOid: UUID,
        godkjent: Boolean
    ): UUID {
        hendelseMediator.håndter(
            godkjenningDTO = GodkjenningDTO(
                oppgaveId,
                godkjent,
                saksbehandlerIdent,
                if (godkjent) null else "årsak",
                null,
                null
            ),
            epost = saksbehandlerEpost,
            oid = saksbehandlerOid
        )
        assertEquals("AvventerSystem", testRapid.inspektør.siste("oppgave_oppdatert").path("status").asText())
        val løsning = testRapid.inspektør.siste("saksbehandler_løsning")
        testRapid.sendTestMessage(løsning.toString())
        return UUID.fromString(løsning.path("@id").asText())
    }

    protected fun assertHendelse(hendelseId: UUID) {
        assertEquals(1, using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT COUNT(1) FROM hendelse WHERE id = ?", hendelseId).map { it.int(1) }.asSingle)
        })
    }

    protected fun assertIkkeHendelse(hendelseId: UUID) {
        assertEquals(0, using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT COUNT(1) FROM hendelse WHERE id = ?", hendelseId).map { it.int(1) }.asSingle)
        })
    }

    protected fun assertVedtak(vedtaksperiodeId: UUID) {
        assertEquals(1, vedtak(vedtaksperiodeId))
    }

    protected fun assertIkkeVedtak(vedtaksperiodeId: UUID) {
        assertEquals(0, vedtak(vedtaksperiodeId))
    }

    protected fun vedtak(vedtaksperiodeId: UUID): Int {
        return using(sessionOf(dataSource)) { session ->
            requireNotNull(
                session.run(
                    queryOf(
                        "SELECT COUNT(*) FROM vedtak WHERE vedtaksperiode_id = ?",
                        vedtaksperiodeId
                    ).map { row -> row.int(1) }.asSingle
                )
            )
        }
    }

    protected fun assertGodkjenningsbehovløsning(
        godkjent: Boolean,
        saksbehandlerIdent: String,
        block: (JsonNode) -> Unit = {}
    ) {
        assertLøsning("Godkjenning") {
            assertTrue(it.path("godkjent").isBoolean)
            assertEquals(godkjent, it.path("godkjent").booleanValue())
            assertEquals(saksbehandlerIdent, it.path("saksbehandlerIdent").textValue())
            assertNotNull(it.path("godkjenttidspunkt").asLocalDateTime())
            block(it)
        }
    }

    protected fun assertAutomatisertLøsning() {
        assertGodkjenningsbehovløsning(true, "Automatisk behandlet") {
            assertTrue(it.path("automatiskBehandling").booleanValue())
        }
    }

    protected fun assertLøsning(behov: String, assertBlock: (JsonNode) -> Unit) {
        testRapid.inspektør.løsning(behov).also(assertBlock)
    }

    protected fun assertBehov(vararg behov: String) {
        assertTrue(testRapid.inspektør.behov().containsAll(behov.toList()))
    }

    protected fun assertIkkeBehov(vararg behov: String) {
        assertFalse(testRapid.inspektør.behov().containsAll(behov.toList()))
    }

    protected fun assertTilstand(hendelseId: UUID, vararg tilstand: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT tilstand FROM command_context WHERE hendelse_id = ? ORDER BY id ASC",
                    hendelseId
                ).map { it.string("tilstand") }.asList
            )
        }.also {
            assertEquals(tilstand.toList(), it)
        }
    }

    protected fun assertOppgaver(antall: Int) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(antall, oppgaver.size)
    }

    protected fun assertOppgave(indeks: Int, vararg status: Oppgavestatus) {
        val oppgaver = testRapid.inspektør.oppgaver()
        assertEquals(status.toList(), oppgaver[indeks])
    }

    private fun TestRapid.RapidInspector.oppgaver(): Map<Int, List<Oppgavestatus>> {
        val oppgaveindekser = mutableListOf<Long>()
        val oppgaver = mutableMapOf<Int, MutableList<JsonNode>>()
        hendelser("oppgave_opprettet")
            .forEach {
                oppgaveindekser.add(it.path("oppgaveId").asLong())
                oppgaver[oppgaveindekser.size - 1] = mutableListOf(it)
            }
        hendelser("oppgave_oppdatert")
            .forEach { oppgave ->
                val indeks = oppgaveindekser.indexOf(oppgave.path("oppgaveId").asLong())
                oppgaver[indeks]?.add(oppgave)
            }
        return oppgaver
            .mapValues { (_, oppgaver) ->
                oppgaver.map { oppgave ->
                    Oppgavestatus.valueOf(oppgave.path("status").asText())
                }
            }
    }

    protected fun assertIngenOppgave() {
        assertEquals(0, testRapid.inspektør.hendelser("oppgave_opprettet").size)
    }

    protected fun assertSnapshot(forventet: String, vedtaksperiodeId: UUID) {
        assertEquals(forventet, using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "SELECT data FROM speil_snapshot WHERE id = (SELECT speil_snapshot_ref FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { it.string("data") }.asSingle
            )
        })
    }

    protected fun assertWarning(forventet: String, vedtaksperiodeId: UUID) {
        assertEquals(forventet, using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "SELECT melding FROM warning WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).map { it.string("melding") }.asList
            )
        }.first())
    }

    protected fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    protected fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    private fun TestRapid.RapidInspector.siste(type: String) =
        hendelser(type).last()

    protected fun TestRapid.RapidInspector.behov() =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").map(JsonNode::asText) }

    protected fun TestRapid.RapidInspector.løsning(behov: String) =
        hendelser("behov")
            .filter { it.hasNonNull("@løsning") }
            .last { it.path("@behov").map(JsonNode::asText).contains(behov) }
            .path("@løsning").path(behov)

    protected fun TestRapid.RapidInspector.contextId() =
        hendelser("behov")
            .last { it.hasNonNull("contextId") }
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    protected fun TestRapid.RapidInspector.oppgaveId() =
        hendelser("oppgave_opprettet")
            .last()
            .path("oppgaveId")
            .asLong()

    protected fun TestRapid.RapidInspector.contextId(hendelseId: UUID) =
        hendelser("behov")
            .last { it.hasNonNull("contextId") && it.path("hendelseId").asText() == hendelseId.toString() }
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    protected fun TestRapid.RapidInspector.oppgaveId(hendelseId: UUID) =
        hendelser("oppgave_opprettet")
            .last { it.path("hendelseId").asText() == hendelseId.toString() }
            .path("oppgaveId")
            .asLong()
}
