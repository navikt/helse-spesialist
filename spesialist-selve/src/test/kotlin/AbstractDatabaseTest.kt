import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.postgresql.ds.PGSimpleDataSource
import org.postgresql.util.PSQLException
import java.io.File
import java.util.*
import javax.sql.DataSource

abstract class AbstractDatabaseTest {

    companion object {

        private val hikariConfig = HikariConfig().apply {
            jdbcUrl = this@Companion.getJdbcUrl()
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 500001
            connectionTimeout = 1000
            maxLifetime = 600001
        }

        private fun getJdbcUrl(): String {
            fun standaloneDataSourceIsRunning(url: String): Boolean {
                val dataSource = PGSimpleDataSource()
                dataSource.setUrl(url)
                return try {
                    dataSource.connection
                    true
                } catch (e: PSQLException) {
                    false
                }
            }

            fun startEmbeddedPostgres(): EmbeddedPostgres {
                val postgresPath = createTempDir()
                return EmbeddedPostgres.builder()
                    .setOverrideWorkingDirectory(postgresPath)
                    .setDataDirectory(postgresPath.resolve("datadir"))
                    .start()
            }

            val urlStandaloneDatabase: String? = File(DATABASE_URL_FILE_PATH).let {
                if (it.exists()) {
                    it.readText()
                } else null
            }

            return if (urlStandaloneDatabase != null && standaloneDataSourceIsRunning(urlStandaloneDatabase)) {
                urlStandaloneDatabase
            } else {
                startEmbeddedPostgres().getJdbcUrl("postgres", "postgres")
            }
        }

        internal val dataSource = HikariDataSource(hikariConfig)

        init {
            Flyway.configure()
                .dataSource(dataSource)
                .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
                .load()
                .migrate()

            createTruncateFunction(dataSource)
        }
    }

    @BeforeEach
    internal fun resetDatabase() {
        sessionOf(dataSource).use  {
            it.run(queryOf("SELECT truncate_tables()").asExecute)
        }
    }
}

internal fun createTruncateFunction(dataSource: DataSource) {
    sessionOf(dataSource).use  {
        @Language("PostgreSQL")
        val query = """
            CREATE OR REPLACE FUNCTION truncate_tables() RETURNS void AS $$
            DECLARE
            truncate_statement text;
            BEGIN
                SELECT 'TRUNCATE ' || string_agg(format('%I.%I', schemaname, tablename), ',') || ' CASCADE'
                    INTO truncate_statement
                FROM pg_tables
                WHERE schemaname='public'
                AND tablename not in ('enhet', 'flyway_schema_history');

                EXECUTE truncate_statement;
            END;
            $$ LANGUAGE plpgsql;
        """
        it.run(queryOf(query).asExecute)
    }
}
