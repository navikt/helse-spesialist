package no.nav.helse.e2e


import com.fasterxml.jackson.databind.JsonNode
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.api.GodkjenningDTO
import no.nav.helse.mediator.kafka.FeatureToggle
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GodkjenningE2ETest {
    private companion object {
        private val SPESIALIST_OID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@nav.no"
        private const val OPPGAVEID = 1L
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private const val SNAPSHOTV1 = """{"version": "this_is_version_1"}"""
        private const val SNAPSHOTV2 = """{"version": "this_is_version_2"}"""
    }

    private val testRapid = TestRapid()
    private val meldingsfabrikk = Testmeldingfabrikk(UNG_PERSON_FNR_2018, AKTØR)
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var hendelseMediator: HendelseMediator
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)

    private fun nyHendelseId() = UUID.randomUUID()

    @BeforeAll
    fun activate() {
        FeatureToggle.nyGodkjenningRiver = true
    }

    @AfterAll
    fun deactivate() {
        FeatureToggle.nyGodkjenningRiver = false
    }

    @Test
    fun `ignorerer endringer på ukjente vedtaksperioder`() {
        val hendelseId = sendVedtaksperiodeEndret()
        assertSpleisbehov(hendelseId)
        assertTilstand(hendelseId, VEDTAKSPERIODE_ID, "NY", "FERDIG")
        verify(exactly = 0) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter ikke vedtak ved godkjenningsbehov uten nødvendig informasjon`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov()
        assertSpleisbehov(godkjenningsmeldingId)
        assertTilstand(godkjenningsmeldingId, VEDTAKSPERIODE_ID, "NY", "SUSPENDERT")
        assertBehov("HentPersoninfo", "HentEnhet", "HentInfotrygdutbetalinger")
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `oppretter vedtak ved godkjenningsbehov`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov()
        sendPersoninfoløsning(godkjenningsmeldingId)
        assertSnapshot(SNAPSHOTV1)
        assertTilstand(godkjenningsmeldingId, VEDTAKSPERIODE_ID, "NY", "SUSPENDERT", "SUSPENDERT")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler)
        assertVedtak(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler godkjenner`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov()
        sendPersoninfoløsning(godkjenningsmeldingId)
        sendSaksbehandlerløsning(OPPGAVEID, true)
        assertSnapshot(SNAPSHOTV1)
        assertTilstand(godkjenningsmeldingId, VEDTAKSPERIODE_ID, "NY", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovLøsning(true)
    }

    @Test
    fun `løser godkjenningsbehov når saksbehandler avslår`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov()
        sendPersoninfoløsning(godkjenningsmeldingId)
        sendSaksbehandlerløsning(OPPGAVEID, false)
        assertSnapshot(SNAPSHOTV1)
        assertTilstand(godkjenningsmeldingId, VEDTAKSPERIODE_ID, "NY", "SUSPENDERT", "SUSPENDERT", "FERDIG")
        assertOppgave(0, Oppgavestatus.AvventerSaksbehandler, Oppgavestatus.Ferdigstilt)
        assertGodkjenningsbehovLøsning(false)
    }

    @Test
    fun `endringer på kjente vedtaksperioder`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returnsMany listOf(SNAPSHOTV1, SNAPSHOTV2)
        val godkjenningsmeldingId = sendGodkjenningsbehov()
        sendPersoninfoløsning(godkjenningsmeldingId)
        val endringsmeldingId = sendVedtaksperiodeEndret()
        assertTilstand(godkjenningsmeldingId, VEDTAKSPERIODE_ID, "NY", "SUSPENDERT", "SUSPENDERT")
        assertTilstand(endringsmeldingId, VEDTAKSPERIODE_ID, "NY", "FERDIG")
        assertSnapshot(SNAPSHOTV2)
        verify(exactly = 2) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `vedtaksperiode forkastet`() {
        val hendelseId = sendVedtaksperiodeForkastet()
        assertSpleisbehov(hendelseId)
        assertTilstand(hendelseId, VEDTAKSPERIODE_ID, "NY", "FERDIG")
        assertIkkeVedtak(VEDTAKSPERIODE_ID)
    }

    private fun sendVedtaksperiodeForkastet() = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeForkastet(id, VEDTAKSPERIODE_ID, ORGNR))
    }

    private fun sendVedtaksperiodeEndret() = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeEndret(id, VEDTAKSPERIODE_ID, ORGNR))
    }

    private fun sendGodkjenningsbehov() = nyHendelseId().also { id ->
        testRapid.sendTestMessage(meldingsfabrikk.lagGodkjenningsbehov(id, VEDTAKSPERIODE_ID, ORGNR))
    }

    private fun sendPersoninfoløsning(spleisbehovId: UUID) = nyHendelseId().also { id ->
        testRapid.sendTestMessage(
            meldingsfabrikk.lagPersoninfoløsning(
                id,
                spleisbehovId,
                testRapid.inspektør.contextId(),
                VEDTAKSPERIODE_ID,
                ORGNR
            )
        )
    }

    private fun sendSaksbehandlerløsning(oppgaveId: Long, godkjent: Boolean) = nyHendelseId().also { id ->
        hendelseMediator.håndter(
            GodkjenningDTO(
                oppgaveId.toString(),
                godkjent,
                SAKSBEHANDLERIDENT,
                if (godkjent) null else "årsak",
                null,
                null
            ), SAKSBEHANDLEREPOST, SAKSBEHANDLEROID
        )
        testRapid.sendTestMessage(
            testRapid.inspektør.meldinger().last { it.path("@event_name").asText() == "saksbehandler_løsning" }
                .toString()
        )
    }

    private fun assertSpleisbehov(hendelseId: UUID) {
        assertEquals(1, using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT COUNT(1) FROM spleisbehov WHERE id = ?", hendelseId).map { it.int(1) }.asSingle)
        })
    }

    private fun assertVedtak(vedtaksperiodeId: UUID) {
        assertEquals(1, vedtak(vedtaksperiodeId))
    }

    private fun assertIkkeVedtak(vedtaksperiodeId: UUID) {
        assertEquals(0, vedtak(vedtaksperiodeId))
    }

    private fun vedtak(vedtaksperiodeId: UUID): Int {
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

    private fun assertGodkjenningsbehovLøsning(godkjent: Boolean) {
        assertLøsning("Godkjenning") {
            assertTrue(it.path("godkjent").isBoolean)
            assertEquals(godkjent, it.path("godkjent").booleanValue())
            assertEquals(SAKSBEHANDLERIDENT, it.path("saksbehandlerIdent").textValue())
            assertNotNull(it.path("godkjenttidspunkt").asLocalDateTime())
        }
    }

    private fun assertLøsning(behov: String, assertBlock: (JsonNode) -> Unit) {
        testRapid.inspektør.løsning(behov).also(assertBlock)
    }

    private fun assertBehov(vararg behov: String) {
        assertTrue(testRapid.inspektør.behov().containsAll(behov.toList()))
    }

    private fun assertIkkeBehov(vararg behov: String) {
        assertFalse(testRapid.inspektør.behov().containsAll(behov.toList()))
    }

    private fun assertTilstand(hendelseId: UUID, vedtaksperiodeId: UUID, vararg tilstand: String) {
        assertEquals(tilstand.toList(), using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "SELECT tilstand FROM command_context WHERE spleisbehov_id = ? AND vedtaksperiode_id = ? ORDER BY id ASC",
                    hendelseId,
                    vedtaksperiodeId
                ).map { it.string("tilstand") }.asList
            )
        })
    }

    private fun assertOppgave(indeks: Int, vararg status: Oppgavestatus) {
        val oppgaver = mutableListOf<Pair<Long, MutableList<JsonNode>>>()
        testRapid.inspektør.hendelser("oppgave_opprettet")
            .forEach { oppgaver.add(it.path("oppgaveId").asLong() to mutableListOf(it)) }
        testRapid.inspektør.hendelser("oppgave_oppdatert")
            .forEach { oppgave ->
                val id = oppgave.path("oppgaveId").asLong()
                oppgaver.firstOrNull { id == it.first }
                    ?.second?.add(oppgave)
            }

        assertEquals(status.toList(), oppgaver[indeks].second.map {
            Oppgavestatus.valueOf(it.path("status").asText())
        })
    }

    private fun assertSnapshot(forventet: String) {
        assertEquals(forventet, using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "SELECT data FROM speil_snapshot WHERE id = (SELECT speil_snapshot_ref FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId)",
                    mapOf(
                        "vedtaksperiodeId" to VEDTAKSPERIODE_ID
                    )
                ).map { it.string("data") }.asSingle
            )
        })
    }

    @BeforeAll
    internal fun setupAll(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection
        val hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
        dataSource = HikariDataSource(hikariConfig)

        hendelseMediator = HendelseMediator(
            rapidsConnection = testRapid,
            speilSnapshotRestClient = restClient,
            dataSource = dataSource,
            spesialistOID = SPESIALIST_OID
        )
    }

    private fun createHikariConfig(jdbcUrl: String) =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

    @AfterAll
    internal fun teardown() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @BeforeEach
    internal fun setupEach() {
        clearMocks(restClient)
        Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to SPESIALIST_OID.toString()))
            .load()
            .also {
                it.clean()
                it.migrate()
            }

        testRapid.reset()
    }

    private fun TestRapid.RapidInspector.meldinger() =
        (0 until size).map { index -> message(index) }

    private fun TestRapid.RapidInspector.hendelser(type: String) =
        meldinger().filter { it.path("@event_name").asText() == type }

    private fun TestRapid.RapidInspector.behov() =
        hendelser("behov")
            .filterNot { it.hasNonNull("@løsning") }
            .flatMap { it.path("@behov").map(JsonNode::asText) }

    private fun TestRapid.RapidInspector.løsning(behov: String) =
        hendelser("behov")
            .filter { it.hasNonNull("@løsning") }
            .last { it.path("@behov").map(JsonNode::asText).contains(behov) }
            .path("@løsning").path(behov)

    private fun TestRapid.RapidInspector.contextId() =
        hendelser("behov")
            .last { it.hasNonNull("contextId") }
            .path("contextId")
            .asText()
            .let { UUID.fromString(it) }

    private fun TestRapid.RapidInspector.oppgaveId() =
        hendelser("oppgave_opprettet")
            .last()
            .path("oppgaveId")
            .asLong()
}
