package no.nav.helse

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.net.ServerSocket
import java.util.*
import javax.sql.DataSource

class TestApplication(private val port: Int = randomPort()) {

    companion object {
        private fun randomPort(): Int = ServerSocket(0).use {
            it.localPort
        }
    }

    private val postgres = PostgreSQLContainer<Nothing>("postgres:14")

    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway
    private lateinit var app: ApplicationEngine
    private lateinit var appBaseUrl: String

    fun start(appBlock: (Application.(dataSource: DataSource) -> Unit)? = {}) {
        postgres.start()

        appBaseUrl = "http://localhost:$port"

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
            initializationFailTimeout = 5000
        })

        flyway = Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
            .load()

        flyway.migrate()

        app = embeddedServer(
            factory = Netty,
            environment = applicationEngineEnvironment {
                KtorConfig(httpPort = port).configure(this)
                module {
                    appBlock?.let { it(dataSource) }
                }
            },
            configure = {
                responseWriteTimeoutSeconds = 30
            }
        )

        app.start(wait = true)
    }

    private class KtorConfig(private val httpPort: Int = 8080) {
        fun configure(builder: ApplicationEngineEnvironmentBuilder) {
            builder.connector {
                port = httpPort
            }
        }
    }

}
