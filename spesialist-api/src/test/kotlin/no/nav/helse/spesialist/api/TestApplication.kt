package no.nav.helse.spesialist.api

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.net.ServerSocket
import java.util.UUID
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

class TestApplication(private val port: Int = randomPort()) {

    companion object {
        private fun randomPort(): Int = ServerSocket(0).use {
            it.localPort
        }
    }

    private val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
        withReuse(true)
        withLabel("app-navn", "spesialist-testapplication")
    }

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
            maximumPoolSize = 5
            connectionTimeout = 500
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
