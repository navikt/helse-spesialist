package no.nav.helse.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.AzureAdAppConfig
import no.nav.helse.OidcDiscovery
import no.nav.helse.azureAdAppAuthentication
import no.nav.helse.objectMapper
import no.nav.helse.tildeling.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.net.ServerSocket
import java.nio.file.Path
import java.util.*
import javax.sql.DataSource
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TildelingApiTest {
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var dataSource: DataSource
    private var vedtakId by Delegates.notNull<Long>()
    private lateinit var tildelingMediator: TildelingMediator
    private lateinit var server: ApplicationEngine

    private val httpPort = ServerSocket(0).use { it.localPort }

    private val jwtStub = JwtStub()
    private val requiredGroup = "required_group"
    private val epostadresse = "epostadresse"
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"
    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort

        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }

    @BeforeAll
    fun setup(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        dataSource = embeddedPostgres.setupDataSource()
        vedtakId = dataSource.opprettVedtak().toLong()
        tildelingMediator = TildelingMediator(dataSource)
        server = embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }

            val oidcDiscovery = OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
            val azureConfig =
                AzureAdAppConfig(
                    clientId = UUID.randomUUID().toString(),
                    speilClientId = clientId,
                    requiredGroup = requiredGroup
                )
            val jwkProvider = jwtStub.getJwkProviderMock()
            azureAdAppAuthentication(oidcDiscovery, azureConfig, jwkProvider)

            routing {
                authenticate("saksbehandler-direkte") {
                    tildelingApi(tildelingMediator)
                }
            }
        }.also {
            it.start(wait = false)
        }
    }

    @Test
    fun `kan tildele en oppgave til seg selv`() {
        val oppgaveId = UUID.randomUUID()
        val saksbehandlerId = UUID.randomUUID()

        dataSource.opprettSaksbehandler(saksbehandlerId)
        dataSource.opprettSaksbehandlerOppgave(oppgaveId, vedtakId)
        val response = runBlocking {
            client.post<HttpResponse>("/api/v1/tildeling/${oppgaveId}/selv") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(saksbehandlerId)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        assertEquals(saksbehandlerId, tildelingMediator.hentSaksbehandlerFor(oppgaveId))
    }

    @Disabled
    @Test
    fun `kan fjerne tildeling av en oppgave`() {
        val oppgaveId = UUID.randomUUID()
        val saksbehandlerId = UUID.randomUUID()

        dataSource.opprettSaksbehandler(saksbehandlerId)
        dataSource.opprettSaksbehandlerOppgave(oppgaveId, vedtakId)
        dataSource.opprettTildeling(oppgaveId, saksbehandlerId)
        val response = runBlocking {
            client.post<HttpResponse>("/api/v1/tildeling/${oppgaveId}/fjern") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(saksbehandlerId)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        assertEquals(null, tildelingMediator.hentSaksbehandlerFor(oppgaveId))
    }

    private fun HttpRequestBuilder.authentication(oid: UUID) {
        header(
            "Authorization",
            "Bearer ${
                jwtStub.getToken(
                    arrayOf(requiredGroup),
                    oid.toString(),
                    epostadresse,
                    clientId,
                    issuer
                )
            }"
        )
    }

    @AfterAll
    fun tearDown() {
        embeddedPostgres.close()
        server.stop(1000, 1000)
    }
}
