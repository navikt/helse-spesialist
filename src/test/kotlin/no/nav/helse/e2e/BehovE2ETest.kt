package no.nav.helse.e2e


import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeEndretMessage
import no.nav.helse.mediator.kafka.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.CommandContextTilstand
import no.nav.helse.modell.CommandContextTilstand.FERDIG
import no.nav.helse.modell.CommandContextTilstand.NY
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.SpleisbehovDao
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
        private val HENDELSE_ID = UUID.randomUUID()
        private const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private val testRapid = TestRapid()
    private val meldingsfabrikk = Testmeldingfabrikk(UNG_PERSON_FNR_2018)
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var behovMediator: SpleisbehovMediator
    private val restClient = mockk<SpeilSnapshotRestClient>(relaxed = true)

    @Test
    fun `vedtaksperiode endret`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagVedtaksperiodeEndret(HENDELSE_ID))
        assertSpleisbehov(HENDELSE_ID)
        assertTilstand(HENDELSE_ID, NY, FERDIG)
    }

    private fun assertSpleisbehov(hendelseId: UUID) {
        assertEquals(1, using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT COUNT(1) FROM spleisbehov WHERE id = ?", hendelseId).map { it.int(1) }.asSingle)
        })
    }

    private fun assertTilstand(hendelseId: UUID, vararg tilstand: CommandContextTilstand) {
        assertEquals(tilstand.toList(), using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "SELECT tilstand FROM command_context WHERE spleisbehov_id = ? ORDER BY id ASC",
                    hendelseId
                ).map { enumValueOf<CommandContextTilstand>(it.string("tilstand")) }.asList
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

        behovMediator = SpleisbehovMediator(
            speilSnapshotRestClient = restClient,
            dataSource = dataSource,
            spesialistOID = SPESIALIST_IOD,
            vedtakDao = VedtakDao(dataSource),
            snapshotDao = SnapshotDao(dataSource),
            commandContextDao = CommandContextDao(dataSource),
            spleisbehovDao = SpleisbehovDao(dataSource)
        )

        behovMediator.init(testRapid)
        NyVedtaksperiodeEndretMessage.VedtaksperiodeEndretRiver(testRapid, behovMediator)
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
