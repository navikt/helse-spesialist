package no.nav.helse.spesialist.db.migrations

fun main() {
    val env = System.getenv()

    val databaseName = env.getRequired("DATABASE_SPESIALIST_DB_MIGRATIONS_DATABASE")
    val gcpProjectId = env.getRequired("GCP_TEAM_PROJECT_ID")
    val databaseRegion = env.getRequired("DATABASE_REGION")
    val databaseInstance = env.getRequired("DATABASE_INSTANCE")
    val databaseUsername = env.getRequired("DATABASE_SPESIALIST_DB_MIGRATIONS_USERNAME")
    val databasePassword = env.getRequired("DATABASE_SPESIALIST_DB_MIGRATIONS_PASSWORD")

    val migrator =
        FlywayMigrator(
            jdbcUrl =
                "jdbc:postgresql:///$databaseName" +
                    "?cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance" +
                    "&socketFactory=com.google.cloud.sql.postgres.SocketFactory",
            username = databaseUsername,
            password = databasePassword,
        )
    migrator.migrate()
}

private fun Map<String, String>.getRequired(key: String): String = requireNotNull(this[key]) { "$key must be set" }
