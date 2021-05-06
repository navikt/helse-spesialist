package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import org.flywaydb.core.Flyway
import java.time.Duration
import javax.sql.DataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration as createDataSource

internal class DataSourceBuilder(private val env: Map<String, String>) {
    private val databaseName =
        requireNotNull(env["DATABASE_NAME"]) { "database name must be set if jdbc url is not provided" }
    private val databaseHost =
        requireNotNull(env["DATABASE_HOST"]) { "database host must be set if jdbc url is not provided" }
    private val databasePort =
        requireNotNull(env["DATABASE_PORT"]) { "database port must be set if jdbc url is not provided" }
    private val vaultMountPath = env["VAULT_MOUNTPATH"]

    private val dbUrl = env["DATABASE_JDBC_URL"] ?: String.format(
        "jdbc:postgresql://%s:%s/%s", databaseHost, databasePort,
        databaseName
    )
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        maximumPoolSize = 5
        minimumIdle = 2
        idleTimeout = Duration.ofMinutes(1).toMillis()
        maxLifetime = idleTimeout * 5
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        leakDetectionThreshold = Duration.ofSeconds(5).toMillis()
    }
    private val hikariMigrationConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        maximumPoolSize = 1
    }

    fun getDataSource() =
        createDataSource(hikariConfig, vaultMountPath, Role.User.asRole(databaseName))

    fun migrate() {
        val dataSource = createDataSource(hikariMigrationConfig, vaultMountPath, Role.Admin.asRole(databaseName))
        runMigration(dataSource, "SET ROLE \"${Role.Admin.asRole(databaseName)}\"")
    }

    private fun runMigration(dataSource: DataSource, initSql: String? = null) =
        Flyway.configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to requireNotNull(env["SPESIALIST_OID"])))
            .initSql(initSql)
            .load()
            .migrate()

    enum class Role {
        Admin, User, ReadOnly;

        fun asRole(databaseName: String) = "$databaseName-${name.toLowerCase()}"
    }
}
