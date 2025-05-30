package no.nav.helse.sidegig

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration

internal class DataSourceBuilder(env: Map<String, String>) {
    private val gcpProjectId: String = requireNotNull(env["GCP_TEAM_PROJECT_ID"]) { "GCP_TEAM_PROJECT_ID must be set" }
    private val databaseRegion: String = requireNotNull(env["DATABASE_REGION"]) { "DATABASE_REGION must be set" }
    private val databaseInstance: String = requireNotNull(env["DATABASE_INSTANCE"]) { "DATABASE_INSTANCE must be set" }
    private val databaseUsername: String =
        requireNotNull(env["DATABASE_SPESIALIST_SIDEGIG_USERNAME"]) { "DATABASE_SPESIALIST_SIDEGIG_USERNAME must be set" }
    private val databasePassword: String =
        requireNotNull(env["DATABASE_SPESIALIST_SIDEGIG_PASSWORD"]) { "DATABASE_SPESIALIST_SIDEGIG_PASSWORD must be set" }
    private val databaseName: String =
        requireNotNull(env["DATABASE_SPESIALIST_SIDEGIG_DATABASE"]) { "DATABASE_SPESIALIST_SIDEGIG_DATABASE must be set" }

    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl =
                String.format(
                    "jdbc:postgresql:///%s?%s&%s",
                    databaseName,
                    "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
                    "socketFactory=com.google.cloud.sql.postgres.SocketFactory",
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
