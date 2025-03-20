package no.nav.helse.spesialist.db.testfixtures

import no.nav.helse.spesialist.db.DBModule
import org.testcontainers.containers.PostgreSQLContainer

class TestcontainersDatabase(moduleLabel: String) {
    private val postgres =
        PostgreSQLContainer("postgres:14")
            .withReuse(true)
            .withLabel("app", "spesialist")
            .withLabel("module", moduleLabel)
            .withLabel("code-location", javaClass.canonicalName)
            .apply {
                start()
                println("Database startet opp.\nUrl: $jdbcUrl\nBrukernavn: $username\nPassord: $password")
            }

    val dbModuleConfiguration =
        DBModule.Configuration(
            jdbcUrl = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password,
        )
}
