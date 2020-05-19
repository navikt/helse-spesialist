package no.nav.helse

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.helse.modell.vedtak.snapshot.ArbeidsgiverFraSpleisDto
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import org.flywaydb.core.Flyway
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.sql.DataSource

internal fun setupDataSourceMedFlyway(): DataSource {
    val embeddedPostgres = EmbeddedPostgres.builder().start()

    val hikariConfig = HikariConfig().apply {
        this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
    }

    val dataSource = HikariDataSource(hikariConfig)

    Flyway.configure()
        .dataSource(dataSource)
        .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
        .load()
        .migrate()

    return dataSource
}

internal fun setupDataSource(): DataSource {
    val embeddedPostgres = EmbeddedPostgres.builder().start()

    val hikariConfig = HikariConfig().apply {
        this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
    }

    return HikariDataSource(hikariConfig)
}

class SpleisMockClient(fødselsnummer: String = "01129342684", aktørId: String = "123", organisasjonsnummer: String = "888") {
    private val queuedResponses: Queue<Path> = LinkedList()
    private var vedtaksperiodeId: UUID = UUID.randomUUID()
    private fun nextResponse(): Path = queuedResponses.poll() ?: VEDTAKSPERIODE_TIL_GODKJENNING

    internal fun setVedtaksperiodeId(vedtaksperiodeId: UUID) {
        this.vedtaksperiodeId = vedtaksperiodeId
    }

    internal fun enqueueResponses(vararg responses: Path) {
        queuedResponses.addAll(responses)
    }

    internal val client = HttpClient(MockEngine) {
        install(JsonFeature) {
            this.serializer = JacksonSerializer()
        }
        engine {
            addHandler { _ ->
                val vedtaksperiode = objectMapper.readValue<ObjectNode>(nextResponse().toFile())
                respond(
                    objectMapper.writeValueAsString(
                        PersonFraSpleisDto(
                            aktørId = aktørId, fødselsnummer = fødselsnummer, arbeidsgivere = listOf(
                                ArbeidsgiverFraSpleisDto(
                                    organisasjonsnummer = organisasjonsnummer,
                                    id = UUID.randomUUID(),
                                    vedtaksperioder = listOf(
                                        vedtaksperiode.deepCopy().put("id", vedtaksperiodeId.toString())
                                    )
                                )
                            )
                        )
                    )
                )
            }
        }
    }


    companion object {
        val VEDTAKSPERIODE_TIL_GODKJENNING = Paths.get("src/test/resources/vedtaksperiode.json")
        val VEDTAKSPERIODE_UTBETALT = Paths.get("src/test/resources/vedtaksperiode_utbetalt.json")
    }
}


internal fun failingHttpClient(): HttpClient {
    return HttpClient(MockEngine) {
        engine {
            addHandler { _ ->
                respond("Failed to execute request", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}

internal fun accessTokenClient() = AccessTokenClient(
    "http://localhost.no",
    "",
    "",
    HttpClient(MockEngine) {
        install(JsonFeature) {
            this.serializer = JacksonSerializer { registerModule(JavaTimeModule()) }
        }
        engine {
            addHandler {
                respond(
                    content = """{"access_token": "token", "expires_in": 3600}""",
                    headers = headersOf("Content-Type" to listOf("application/json"))
                )
            }
        }
    }
)
