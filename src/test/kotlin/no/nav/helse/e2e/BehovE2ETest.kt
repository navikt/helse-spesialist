package no.nav.helse.e2e


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
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehovE2ETest {
    private companion object {
        private val SPESIALIST_IOD = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private const val SNAPSHOTV1 = """{"version": "this_is_version_1"}"""
        private const val SNAPSHOTV2 = """{"version": "this_is_version_2"}"""
    }

    private val testRapid = TestRapid()
    private val meldingsfabrikk = Testmeldingfabrikk(UNG_PERSON_FNR_2018, AKTØR)
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var behovMediator: HendelseMediator
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)

    private fun nyHendelseId() = UUID.randomUUID()

    @Test
    fun `ignorerer endringer på ukjente vedtaksperioder`() {
        val hendelseId = sendVedtaksperiodeEndret()
        assertSpleisbehov(hendelseId)
        assertTilstand(hendelseId, VEDTAKSPERIODE_ID, "NY", "FERDIG")
        verify(exactly = 0) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `oppretter ikke vedtak ved godkjenningsbehov uten nødvendig informasjon`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov()
        assertSpleisbehov(godkjenningsmeldingId)
        verify(exactly = 0) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `oppretter vedtak ved godkjenningsbehov`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov()
        sendPersoninfoløsning(godkjenningsmeldingId)
        assertSnapshot(SNAPSHOTV1)
        verify(exactly = 1) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `endringer på kjente vedtaksperioder`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returnsMany listOf(SNAPSHOTV1, SNAPSHOTV2)
        val godkjenningsmeldingId = sendGodkjenningsbehov()
        sendPersoninfoløsning(godkjenningsmeldingId)
        val endringsmeldingId = sendVedtaksperiodeEndret()
        assertTilstand(godkjenningsmeldingId, VEDTAKSPERIODE_ID) // ingen tilstand før godkjenningsbehov er portert over
        assertTilstand(endringsmeldingId, VEDTAKSPERIODE_ID, "NY", "FERDIG") // ingen tilstand før godkjenningsbehov er portert over
        assertSnapshot(SNAPSHOTV2)
        verify(exactly = 2) { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) }
    }

    @Test
    fun `vedtaksperiode forkastet`() {
        val hendelseId = sendVedtaksperiodeForkastet()
        assertSpleisbehov(hendelseId)
        assertTilstand(hendelseId, VEDTAKSPERIODE_ID, "NY", "FERDIG")
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
        testRapid.sendTestMessage(meldingsfabrikk.lagPersoninfoløsning(id, spleisbehovId, VEDTAKSPERIODE_ID, ORGNR))
    }

    private fun assertSpleisbehov(hendelseId: UUID) {
        assertEquals(1, using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT COUNT(1) FROM spleisbehov WHERE id = ?", hendelseId).map { it.int(1) }.asSingle)
        })
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

        behovMediator = HendelseMediator(
            rapidsConnection = testRapid,
            speilSnapshotRestClient = restClient,
            dataSource = dataSource,
            spesialistOID = SPESIALIST_IOD
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
            .placeholders(mapOf("spesialist_oid" to SPESIALIST_IOD.toString()))
            .load()
            .also {
                it.clean()
                it.migrate()
            }

        testRapid.reset()
    }
}
