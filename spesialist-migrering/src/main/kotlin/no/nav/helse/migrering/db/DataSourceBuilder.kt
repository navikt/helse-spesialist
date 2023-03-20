package no.nav.helse.migrering.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration

// Understands how to create a data source from environment variables
internal class DataSourceBuilder(env: Map<String, String>, dbUser: DbUser) {

    internal enum class DbUser(private val user: String) {
        SPESIALIST("SPESIALIST_MIGRERING");

        override fun toString(): String {
            return user
        }
    }

    private val gcpProjectId: String = requireNotNull(env["GCP_TEAM_PROJECT_ID"]) { "GCP_TEAM_PROJECT_ID must be set" }
    private val databaseRegion: String = requireNotNull(env["DATABASE_${dbUser}_REGION"]) { "REGION must be set" }
    private val databaseInstance: String = requireNotNull(env["DATABASE_${dbUser}_INSTANCE"]) { "INSTANCE must be set" }
    private val databaseUsername: String = requireNotNull(env["DATABASE_${dbUser}_USERNAME"]) { "USERNAME must be set" }
    private val databasePassword: String = requireNotNull(env["DATABASE_${dbUser}_PASSWORD"]) { "PASSWORD must be set" }
    private val databaseName: String = requireNotNull(env["DATABASE_${dbUser}_DATABASE"]) { "DATABASE must be set" }

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = String.format(
            "jdbc:postgresql:///%s?%s&%s",
            databaseName,
            "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
            "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
        )

        username = databaseUsername
        password = databasePassword

        maximumPoolSize = 3
        minimumIdle = 1
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    internal fun getDataSource() = HikariDataSource(hikariConfig)
}
