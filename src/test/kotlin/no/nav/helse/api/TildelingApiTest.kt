package no.nav.helse.api

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.call.*
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
import no.nav.helse.modell.feilhåndtering.FeilDto
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.objectMapper
import no.nav.helse.tildeling.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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


    private val SAKSBEHANDLERIOD = UUID.randomUUID()
    private val SAKSBEHANDLER_EPOST = "sara.saksbehandler@nav.no"
    private val SAKSBEHANDLER_NAVN = "Sara Saksbehandler"

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var dataSource: DataSource
    private var vedtakId by Delegates.notNull<Long>()
    private lateinit var saksbehandlerDao: SaksbehandlerDao
    private lateinit var tildelingDao: TildelingDao
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
        expectSuccess = false
        install(JsonFeature) {
            serializer = JacksonSerializer(jackson = objectMapper)
        }
    }

    @BeforeAll
    fun setup(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        dataSource = embeddedPostgres.setupDataSource()
        vedtakId = dataSource.opprettVedtak()
        saksbehandlerDao = SaksbehandlerDao(dataSource)
        tildelingDao = TildelingDao(dataSource)
        tildelingMediator = TildelingMediator(saksbehandlerDao, tildelingDao)
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
        val oppgavereferanse = UUID.randomUUID()

        opprettSaksBehandler()
        val oppgaveId = dataSource.opprettSaksbehandlerOppgave(oppgavereferanse, vedtakId)!!
        val response = runBlocking {
            client.post<HttpResponse>("/api/v1/tildeling/${oppgaveId}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLERIOD)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        assertEquals(SAKSBEHANDLER_NAVN, tildelingDao.finnSaksbehandlerNavn(oppgaveId))
    }

    @Test
    fun `kan fjerne tildeling av en oppgave`() {
        val hendelseId = UUID.randomUUID()

        opprettSaksBehandler()
        val oppgaveId = dataSource.opprettSaksbehandlerOppgave(hendelseId, vedtakId)!!
        tildelingDao.opprettTildeling(oppgaveId, SAKSBEHANDLERIOD)
        val response = runBlocking {
            client.delete<HttpResponse>("/api/v1/tildeling/${oppgaveId}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(SAKSBEHANDLERIOD)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        assertEquals(null, tildelingDao.finnSaksbehandlerNavn(oppgaveId))
    }

    @Test
    fun `Gir feil hvis bruker forsøker å tildele en oppgave som allerede er tildelt`() {

        val hendelseId = UUID.randomUUID()
        val saksbehandlerId = UUID.randomUUID()

        dataSource.opprettSaksbehandler(saksbehandlerId)
        val oppgaveId = dataSource.opprettSaksbehandlerOppgave(hendelseId, vedtakId)!!
        dataSource.opprettTildeling(oppgaveId, saksbehandlerId)

        val response = runBlocking {
            client.post<HttpResponse>("/api/v1/tildeling/${oppgaveId}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = objectMapper.createObjectNode()
                authentication(saksbehandlerId)
            }
        }

        assertEquals(response.status, HttpStatusCode.Conflict)
        val feilDto = runBlocking { response.receive<FeilDto>() }
        assertEquals(feilDto.feilkode, OppgaveErAlleredeTildelt("navn").feilkode)
        assertEquals("Sara Saksbehandler", feilDto.kontekst["tildeltTil"])
    }

    private fun opprettSaksBehandler() =
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLERIOD, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST)

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
