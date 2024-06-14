package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import org.flywaydb.core.Flyway
import java.time.Duration
import javax.sql.DataSource

internal class DataSourceBuilder(env: Map<String, String>) {
    private val databaseHost: String = requireNotNull(env["DATABASE_HOST"]) { "host må settes" }
    private val databasePort: String = requireNotNull(env["DATABASE_PORT"]) { "port må settes" }
    private val databaseName: String = requireNotNull(env["DATABASE_DATABASE"]) { "databasenavn må settes" }
    private val databaseUsername: String = requireNotNull(env["DATABASE_USERNAME"]) { "brukernavn må settes" }
    private val databasePassword: String = requireNotNull(env["DATABASE_PASSWORD"]) { "passord må settes" }

    private val dbUrl =
        String.format(
            "jdbc:postgresql://%s:%s/%s",
            databaseHost,
            databasePort,
            databaseName,
        )

    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = dbUrl
            username = databaseUsername
            password = databasePassword
            maximumPoolSize = 20
            minimumIdle = 2
            idleTimeout = Duration.ofMinutes(1).toMillis()
            maxLifetime = idleTimeout * 5
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            connectionTimeout = Duration.ofSeconds(5).toMillis()
            leakDetectionThreshold = Duration.ofSeconds(30).toMillis()
            metricRegistry =
                PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    CollectorRegistry.defaultRegistry,
                    Clock.SYSTEM,
                )
        }

    private val hikariMigrationConfig =
        HikariConfig().apply {
            jdbcUrl = dbUrl
            username = databaseUsername
            password = databasePassword
            connectionTimeout = Duration.ofSeconds(5).toMillis()
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            maximumPoolSize = 2
        }

    private fun runMigration(dataSource: DataSource) =
        Flyway.configure()
            .dataSource(dataSource)
            .lockRetryCount(-1)
            .load()
            .migrate()

    internal fun getDataSource(): HikariDataSource {
        return HikariDataSource(hikariConfig)
    }

    internal fun migrate() {
        val dataSource = HikariDataSource(hikariMigrationConfig)
        dataSource.use {
            runMigration(it)
        }
    }
}
