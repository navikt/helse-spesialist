package no.nav.helse.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.client.statement.HttpStatement
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.jackson.JacksonConverter
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import no.nav.helse.*
import no.nav.helse.accessTokenClient
import no.nav.helse.httpClientForSpleis
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dto.PersonForSpeilDto
import no.nav.helse.modell.dto.SaksbehandleroppgaveDto
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.rapids_rivers.inMemoryRapid
import no.nav.helse.setupDataSource
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.io.TempDir
import java.net.ServerSocket
import java.nio.file.Path
import java.sql.Connection
import java.time.Duration
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import javax.sql.DataSource

@TestInstance(Lifecycle.PER_CLASS)
internal class RestApiTest {
    private companion object {
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource

    private lateinit var app: ApplicationEngine
    private lateinit var spleisbehovMediator: SpleisbehovMediator
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val jwtStub = JwtStub()
    private val requiredGroup = "required_group"
    private val navIdent = "abcd"
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"
    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort
            header(
                "Authorization",
                "Bearer ${jwtStub.getToken(arrayOf(requiredGroup), navIdent, clientId, issuer)}".also { println(it) })
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }

    @BeforeAll
    internal fun `start embedded environment`(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        dataSource = setupDataSource()

        val personDao = PersonDao(dataSource)
        val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
        val vedtakDao = VedtakDao(dataSource)
        val spleisbehovDao = SpleisbehovDao(dataSource)
        val snapshotDao = SnapshotDao(dataSource)
        val oppgaveDao = OppgaveDao(dataSource)
        val speilSnapshotRestDao = SpeilSnapshotRestDao(httpClientForSpleis(), accessTokenClient(), "spleisClientId")

        val rapid = inMemoryRapid { }
        spleisbehovMediator = SpleisbehovMediator(
            spleisbehovDao = spleisbehovDao, personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            rapidsConnection = rapid
        )

        val oidcDiscovery = OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
        val azureConfig = AzureAdAppConfig(clientId = clientId, requiredGroup = requiredGroup)
        val jwkProvider = jwtStub.getJwkProviderMock()

        app = embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            azureAdAppAuthentication(oidcDiscovery, azureConfig, jwkProvider)
            oppgaveApi(oppgaveDao, personDao, vedtakDao)
            vedtaksperiodeApi(personDao, vedtakDao, snapshotDao, arbeidsgiverDao, spleisbehovMediator)
        }

        app.start(wait = false)
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        app.stop(1L, 1L, SECONDS)
        postgresConnection.close()
        embeddedPostgres.close()
    }


    @Test
    fun `hent oppgaver`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning("Test", null, "Testsen")
        )
        val response = runBlocking { client.get<HttpStatement>("/api/oppgaver").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val oppgaver = runBlocking { response.receive<List<SaksbehandleroppgaveDto>>() }
        assertEquals(vedtaksperiodeId, oppgaver.first().vedtaksperiodeId)
    }

    @Test
    fun `hent vedtaksperiode`() {
        val vedtaksperiodeId = UUID.fromString("a9951ae7-52ca-4b05-9219-eebbbc73a9dc")
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31)
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            HentPersoninfoLøsning("Test", null, "Testsen")
        )
        val response = runBlocking { client.get<HttpStatement>("/api/vedtaksperiode/$vedtaksperiodeId").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val personForSpeilDto = runBlocking { response.receive<PersonForSpeilDto>() }
        assertEquals(
            vedtaksperiodeId.toString(),
            personForSpeilDto.arbeidsgivere.first().vedtaksperioder.first()["id"].asText()
        )
    }

//    @Test
//    fun `godkjenning av vedtaksperiode`() {
//        val vedtaksperiodeId = UUID.fromString("a9951ae7-52ca-4b05-9219-eebbbc73a9dc")
//        val spleisbehovId = UUID.randomUUID()
//        val godkjenningMessage = GodkjenningMessage(
//            id = spleisbehovId,
//            fødselsnummer = "12345",
//            aktørId = "12345",
//            organisasjonsnummer = "89123",
//            vedtaksperiodeId = vedtaksperiodeId,
//            periodeFom = LocalDate.of(2018, 1, 1),
//            periodeTom = LocalDate.of(2018, 1, 31)
//        )
//        spleisbehovMediator.håndter(godkjenningMessage, "{}")
//        spleisbehovMediator.håndter(
//            spleisbehovId,
//            HentEnhetLøsning("1234"),
//            HentPersoninfoLøsning("Test", null, "Testsen")
//        )
//        val response = runBlocking {
//            client.post<HttpStatement>("/api/vedtaksperiode/$spleisbehovId/vedtak") {
//                body = TextContent(
//                    objectMapper.writeValueAsString(Godkjenning(true)), contentType = ContentType.Application.Json
//                )
//            }.execute()
//        }
//        assertEquals(HttpStatusCode.OK, response.status)
//    }
}
