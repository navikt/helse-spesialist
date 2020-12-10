import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import java.util.*

abstract class AbstractDatabaseTest {

    companion object {
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

        init {
            Flyway.configure()
                .dataSource(dataSource)
                .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
                .load()
                .migrate()
        }
    }

    @BeforeEach
    internal fun resetDatabase() {
        using(sessionOf(dataSource)) {
            @Language("PostgreSQL")
            val query = """
                do
                ${'$'}${'$'}
                declare
                  truncate_stmt text;
                begin
                  select 'truncate ' || string_agg(format('%I.%I', schemaname, tablename), ',')
                    into truncate_stmt
                  from pg_tables
                  where tablename not in ('enhet','flyway_schema_history') and schemaname in ('public');
                  execute truncate_stmt;
                end;
                ${'$'}${'$'}
            """
            it.run(queryOf(query).asExecute)
        }
    }
}
