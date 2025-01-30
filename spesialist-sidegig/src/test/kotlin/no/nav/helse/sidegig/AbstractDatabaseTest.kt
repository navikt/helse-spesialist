package no.nav.helse.sidegig

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

internal abstract class AbstractDatabaseTest {
    protected val behandlingDao = PgBehandlingDao(dataSource)

    protected companion object {
        private val postgres =
            PostgreSQLContainer<Nothing>("postgres:14").apply {
                withReuse(true)
                withLabel("app-navn", "spesialist-sidegig")
                start()

                println("Database: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
            }

        val dataSource =
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = postgres.jdbcUrl
                    username = postgres.username
                    password = postgres.password
                    maximumPoolSize = 5
                    connectionTimeout = 500
                    initializationFailTimeout = 5000
                },
            )

        init {
            Flyway
                .configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }
    }
}

