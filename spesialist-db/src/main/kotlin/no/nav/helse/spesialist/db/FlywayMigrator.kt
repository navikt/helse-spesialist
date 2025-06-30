package no.nav.helse.spesialist.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.time.Duration

class FlywayMigrator(configuration: DBModule.Configuration) {
    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = configuration.jdbcUrl
            username = configuration.username
            password = configuration.password
            connectionTimeout = Duration.ofSeconds(5).toMillis()
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            maximumPoolSize = 2
        }

    fun migrate() {
        HikariDataSource(hikariConfig).use { dataSource ->
            Flyway.configure()
                .dataSource(dataSource)
                .lockRetryCount(-1)
                .load()
                .migrate()
        }
    }
}
