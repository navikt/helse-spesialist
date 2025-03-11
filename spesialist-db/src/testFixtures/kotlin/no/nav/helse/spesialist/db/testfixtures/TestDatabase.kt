package no.nav.helse.spesialist.db.testfixtures

import no.nav.helse.spesialist.db.bootstrap.DBModule
import org.testcontainers.containers.PostgreSQLContainer

object TestDatabase {
    private val postgres =
        PostgreSQLContainer<Nothing>("postgres:14").apply {
            withReuse(true)
            withLabel("app-navn", "spesialist-localapp")
            start()

            println("Database localapp: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
        }

    val dbModuleConfiguration =
        DBModule.Configuration(
            jdbcUrl = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password,
        )
}
