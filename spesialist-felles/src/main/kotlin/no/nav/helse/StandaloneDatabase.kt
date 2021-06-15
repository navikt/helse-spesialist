package no.nav.helse

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File

val DATABASE_URL_FILE_PATH = "${System.getProperty("java.io.tmpdir")}spesialist_standalone_db_url"

object PostgresContainer {
    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:12.1").apply {
            start()
        }
    }
}

internal fun main() {
    val postgresPath = createTempDir()
    val embeddedPostgres = EmbeddedPostgres.builder()
        .setOverrideWorkingDirectory(postgresPath)
        .setDataDirectory(postgresPath.resolve("datadir"))
        .start()

    embeddedPostgres.getJdbcUrl("postgres", "postgres").let { jdbcUrl ->
        File(DATABASE_URL_FILE_PATH).writeText(jdbcUrl)
        println("Url ($jdbcUrl) er skrevet til $DATABASE_URL_FILE_PATH")
    }

    while (true) {
        Thread.sleep(1000)
    }
}
