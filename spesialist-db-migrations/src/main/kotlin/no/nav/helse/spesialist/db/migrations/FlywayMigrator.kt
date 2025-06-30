package no.nav.helse.spesialist.db.migrations

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.time.Duration

class FlywayMigrator(
    val jdbcUrl: String,
    val username: String,
    val password: String,
) {
    private val hikariConfig =
        HikariConfig().also {
            it.jdbcUrl = jdbcUrl
            it.username = username
            it.password = password
            it.connectionTimeout = Duration.ofSeconds(5).toMillis()
            it.initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            it.maximumPoolSize = 2
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
