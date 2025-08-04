package no.nav.helse.spesialist.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import java.time.Duration

internal class DataSourceBuilder(
    configuration: DBModule.Configuration,
) {
    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = configuration.jdbcUrl
            username = configuration.username
            password = configuration.password
            maximumPoolSize = 20
            minimumIdle = 2
            idleTimeout = Duration.ofMinutes(1).toMillis()
            maxLifetime = idleTimeout * 5
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            connectionTimeout = Duration.ofSeconds(5).toMillis()
            leakDetectionThreshold = Duration.ofSeconds(30).toMillis()
            metricRegistry =
                PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    PrometheusRegistry.defaultRegistry,
                    Clock.SYSTEM,
                )
        }

    fun build(): HikariDataSource = HikariDataSource(hikariConfig)
}
