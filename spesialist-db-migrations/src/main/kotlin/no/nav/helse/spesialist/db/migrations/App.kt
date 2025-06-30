package no.nav.helse.spesialist.db.migrations

fun main() {
    val env = System.getenv()
    val migrator =
        FlywayMigrator(
            jdbcUrl =
                "jdbc:postgresql://" +
                    env.getValue("DATABASE_SPESIALIST_DB_MIGRATIONS_HOST") +
                    ":" +
                    env.getValue("DATABASE_SPESIALIST_DB_MIGRATIONS_PORT") +
                    "/" +
                    env.getValue("DATABASE_SPESIALIST_DB_MIGRATIONS_DATABASE"),
            username = env.getValue("DATABASE_SPESIALIST_DB_MIGRATIONS_USERNAME"),
            password = env.getValue("DATABASE_SPESIALIST_DB_MIGRATIONS_PASSWORD"),
        )
    migrator.migrate()
}
