package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import java.time.Duration
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration as createDataSource

internal abstract class DataSourceBuilder(private val spesialistOid: String) {
    protected val hikariConfig = HikariConfig().apply {
        configure(this)
        maximumPoolSize = 5
        minimumIdle = 2
        idleTimeout = Duration.ofMinutes(1).toMillis()
        maxLifetime = idleTimeout * 5
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        leakDetectionThreshold = Duration.ofSeconds(5).toMillis()
        metricRegistry = PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT,
            CollectorRegistry.defaultRegistry,
            Clock.SYSTEM
        )
    }

    protected abstract fun configure(hikariConfig: HikariConfig)

    protected val hikariMigrationConfig = HikariConfig().apply {
        configure(this)
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        maximumPoolSize = 1
    }

    abstract fun getDataSource(): HikariDataSource

    abstract fun migrate()

    protected fun runMigration(dataSource: DataSource, initSql: String? = null) =
        Flyway.configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to spesialistOid))
            .initSql(initSql)
            .lockRetryCount(-1)
            .load()
            .migrate()
}

internal class GcpDataSourceBuilder(env: Map<String, String>) : DataSourceBuilder(requireNotNull(env["SPESIALIST_OID"])) {
    private val databaseHost: String = requireNotNull(env["DATABASE_HOST"]) { "host må settes" }
    private val databasePort: String = requireNotNull(env["DATABASE_PORT"]) { "port må settes" }
    private val databaseName: String = requireNotNull(env["DATABASE_DATABASE"]) { "databasenavn må settes" }
    private val databaseUsername: String = requireNotNull(env["DATABASE_USERNAME"]) { "brukernavn må settes" }
    private val databasePassword: String = requireNotNull(env["DATABASE_PASSWORD"]) { "passord må settes" }

    override fun getDataSource(): HikariDataSource {
        return HikariDataSource(hikariConfig)
    }

    override fun migrate() {
        val dataSource = HikariDataSource(hikariMigrationConfig)
        runMigration(dataSource, null)
        dataSource.close()
    }

    private val dbUrl = String.format(
        "jdbc:postgresql://%s:%s/%s", databaseHost, databasePort, databaseName
    )

    override fun configure(hikariConfig: HikariConfig) {
        hikariConfig.apply {
            jdbcUrl = dbUrl
            username = databaseUsername
            password = databasePassword
        }
    }
}

internal class OnPremDataSourceBuilder(env: Map<String, String>) :
    DataSourceBuilder(requireNotNull(env["SPESIALIST_OID"])) {
    private val databaseName = requireNotNull(env["DATABASE_NAME"]) { "database name must be set if jdbc url is not provided" }
    private val databaseHost = requireNotNull(env["DATABASE_HOST"]) { "database host must be set if jdbc url is not provided" }
    private val databasePort = requireNotNull(env["DATABASE_PORT"]) { "database port must be set if jdbc url is not provided" }
    private val vaultMountPath = env["VAULT_MOUNTPATH"]

    private val dbUrl = String.format(
        "jdbc:postgresql://%s:%s/%s", databaseHost, databasePort, databaseName
    )

    override fun getDataSource(): HikariDataSource =
        createDataSource(hikariConfig, vaultMountPath, Role.User.asRole(databaseName))

    override fun migrate() {
        val dataSource = createDataSource(hikariMigrationConfig, vaultMountPath, Role.Admin.asRole(databaseName))
        runMigration(dataSource, "SET ROLE \"${Role.Admin.asRole(databaseName)}\"")
        dataSource.close()
    }

    override fun configure(hikariConfig: HikariConfig) {
        hikariConfig.apply {
            jdbcUrl = dbUrl
        }
    }

    enum class Role {
        Admin, User;

        fun asRole(databaseName: String) = "$databaseName-${name.lowercase()}"
    }
}
