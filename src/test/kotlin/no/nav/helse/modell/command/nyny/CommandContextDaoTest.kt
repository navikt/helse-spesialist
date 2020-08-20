package no.nav.helse.modell.command.nyny

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.CommandContextTilstand
import no.nav.helse.modell.CommandContextTilstand.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.util.*
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CommandContextDaoTest {

    private companion object {
        private val SPESIALIST_IOD = UUID.randomUUID()
        private val FNR = "FNR"
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val HENDELSE = UUID.randomUUID()
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var commandContextDao: CommandContextDao

    private fun context(id: UUID) = CommandContext(id)

    @Test
    fun `lagrer og finner context i db`() {
        val contextId = UUID.randomUUID()
        commandContextDao.lagre(TestHendelse(HENDELSE, VEDTAKSPERIODE), context(contextId), NY)
        assertNotNull(commandContextDao.finn(contextId))
    }

    @Test
    fun `avbryter ikke seg selv`() {
        val context = context(UUID.randomUUID())
        commandContextDao.lagre(TestHendelse(HENDELSE, VEDTAKSPERIODE), context, NY)
        commandContextDao.avbryt(context, VEDTAKSPERIODE)
        assertEquals(NY, status(VEDTAKSPERIODE)[context.id])
    }

    @Test
    fun `avbryter command som er NY eller SUSPENDERT`() {
        val context1 = context(UUID.randomUUID())
        val context2 = context(UUID.randomUUID())
        val context3 = context(UUID.randomUUID())
        commandContextDao.lagre(TestHendelse(HENDELSE, VEDTAKSPERIODE), context2, NY)
        commandContextDao.lagre(TestHendelse(HENDELSE, VEDTAKSPERIODE), context3, SUSPENDERT)
        commandContextDao.avbryt(context1, VEDTAKSPERIODE)
        assertEquals(AVBRUTT, status(VEDTAKSPERIODE)[context2.id])
        assertEquals(AVBRUTT, status(VEDTAKSPERIODE)[context3.id])
    }

    @Test
    fun `avbryter ikke commands som er FEIL eller FERDIG`() {
        val context1 = context(UUID.randomUUID())
        val context2 = context(UUID.randomUUID())
        val context3 = context(UUID.randomUUID())
        commandContextDao.lagre(TestHendelse(HENDELSE, VEDTAKSPERIODE), context2, FERDIG)
        commandContextDao.lagre(TestHendelse(HENDELSE, VEDTAKSPERIODE), context3, FEIL)
        commandContextDao.avbryt(context1, VEDTAKSPERIODE)
        assertEquals(FERDIG, status(VEDTAKSPERIODE)[context2.id])
        assertEquals(FEIL, status(VEDTAKSPERIODE)[context3.id])
    }

    private fun status(vedtaksperiodeId: UUID): Map<UUID, CommandContextTilstand> {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf("SELECT context_id, tilstand FROM command_context WHERE vedtaksperiode_id = ?", vedtaksperiodeId).map { row ->
                    UUID.fromString(row.string("context_id")) to enumValueOf<CommandContextTilstand>(row.string("tilstand"))
                }.asList
            ).toMap()
        }
    }

    private fun testSpleisbehov(hendelseId: UUID = HENDELSE) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO spleisbehov(id, data, original, spleis_referanse, type) VALUES(?, ?::json, ?::json, ?, ?)",
                    hendelseId,
                    "{}",
                    "{}",
                    hendelseId,
                    "Godkjenningsbehov"
                ).asExecute
            )
        }
    }

    private class TestHendelse(override val id: UUID, private val vedtaksperiodeId: UUID) : Hendelse {
        override fun execute(context: CommandContext): Boolean {
            TODO("Not yet implemented")
        }

        override fun fødselsnummer(): String {
            return FNR
        }

        override fun vedtaksperiodeId(): UUID {
            return vedtaksperiodeId
        }

        override fun toJson(): String {
            TODO("Not yet implemented")
        }
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
        commandContextDao = CommandContextDao(dataSource)
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
        testSpleisbehov()
    }
}
