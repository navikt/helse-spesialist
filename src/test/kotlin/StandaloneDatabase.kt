import com.opentable.db.postgres.embedded.EmbeddedPostgres

fun main() {
    val postgresPath = createTempDir()
    EmbeddedPostgres.builder()
        .setPort(13337)
        .setOverrideWorkingDirectory(postgresPath)
        .setDataDirectory(postgresPath.resolve("datadir"))
        .start()

    while (true) {
        Thread.sleep(1000)
    }
}
