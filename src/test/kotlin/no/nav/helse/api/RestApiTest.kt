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
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.vedtak.SaksbehandleroppgaveDto
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.vedtaksperiode.PersonForSpeilDto
import no.nav.helse.vedtaksperiode.VedtaksperiodeMediator
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.io.TempDir
import java.net.ServerSocket
import java.nio.file.Path
import java.sql.Connection
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import javax.sql.DataSource

@TestInstance(Lifecycle.PER_CLASS)
internal class RestApiTest {
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource

    private lateinit var app: ApplicationEngine
    private lateinit var spleisbehovMediator: SpleisbehovMediator
    private lateinit var flyway: Flyway
    private val rapid = TestRapid()
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val jwtStub = JwtStub()
    private val requiredGroup = "required_group"
    private val saksbehandlerIdent = "1234"
    private val epostadresse = "epostadresse"
    private val clientId = "client_id"
    private val oid: UUID = UUID.randomUUID()
    private val spesialistOID: UUID = UUID.randomUUID()
    private val issuer = "https://jwt-provider-domain"
    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort
            header(
                "Authorization",
                "Bearer ${jwtStub.getToken(
                    arrayOf(requiredGroup),
                    oid.toString(),
                    epostadresse,
                    clientId,
                    issuer
                )}"
            )
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }
    var vedtaksperiodeId = UUID.randomUUID()
    private val spleisMockClient = SpleisMockClient()

    @BeforeAll
    internal fun `start embedded environment`(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        dataSource = setupDataSource()

        flyway = Flyway.configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
            .load()

        val speilSnapshotRestClient = SpeilSnapshotRestClient(
            spleisMockClient.client,
            accessTokenClient(),
            "spleisClientId"
        )

        spleisbehovMediator = SpleisbehovMediator(
            dataSource = dataSource,
            speilSnapshotRestClient = speilSnapshotRestClient,
            spesialistOID = spesialistOID
        ).apply { init(rapid) }
        val oppgaveMediator = OppgaveMediator(dataSource)
        val vedtaksperiodeMediator = VedtaksperiodeMediator(
            dataSource = dataSource
        )

        val oidcDiscovery = OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
        val azureConfig = AzureAdAppConfig(clientId = clientId, requiredGroup = requiredGroup)
        val jwkProvider = jwtStub.getJwkProviderMock()

        app = embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            basicAuthentication("🅱️")
            azureAdAppAuthentication(oidcDiscovery, azureConfig, jwkProvider)
            oppgaveApi(oppgaveMediator)
            vedtaksperiodeApi(vedtaksperiodeMediator, spleisbehovMediator, dataSource)
        }

        app.start(wait = false)
    }

    @BeforeEach
    internal fun updateVedtaksperiodeId() {
        flyway.clean()
        flyway.migrate()
        spleisMockClient.setVedtaksperiodeId(vedtaksperiodeId)
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        app.stop(1L, 1L, SECONDS)
        postgresConnection.close()
        embeddedPostgres.close()
    }


    @Test
    fun `hent oppgaver`() {
        val spleisbehovId = UUID.randomUUID()
        val boenhetId = "1234"
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning(boenhetId),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/oppgaver").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val oppgaver = runBlocking { response.receive<List<SaksbehandleroppgaveDto>>() }
        assertTrue(oppgaver.any { it.vedtaksperiodeId == vedtaksperiodeId })
        assertTrue(oppgaver.any { it.boenhet.id == boenhetId })
    }

    @Test
    fun `PersonDTO inneholder infotrygdutbetalinger`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(
                infotrygdutbetalingerLøsning(
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 1, 31),
                    grad = 100,
                    dagsats = 1200.0,
                    typetekst = "ArbRef",
                    orgnr = "89123"
                )
            )
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/$vedtaksperiodeId").execute() }
        val infotrygdutbetalinger =
            runBlocking { requireNotNull(response.receive<PersonForSpeilDto>().infotrygdutbetalinger) }
        assertNotNull(infotrygdutbetalinger)
        assertEquals(LocalDate.of(2018, 1, 1), infotrygdutbetalinger[0]["fom"].asLocalDate())
        assertEquals(LocalDate.of(2018, 1, 31), infotrygdutbetalinger[0]["tom"].asLocalDate())
        assertEquals(100, infotrygdutbetalinger[0]["grad"].asInt())
        assertEquals(1200.0, infotrygdutbetalinger[0]["dagsats"].asDouble())
        assertEquals("ArbRef", infotrygdutbetalinger[0]["typetekst"].asText())
        assertEquals("89123", infotrygdutbetalinger[0]["organisasjonsnummer"].asText())
    }

    @Test
    fun `PersonDTO inneholder enhetsinfo`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("301"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/$vedtaksperiodeId").execute() }
        val enhet = runBlocking { requireNotNull(response.receive<PersonForSpeilDto>().enhet) }
        assertNotNull(enhet)
        assertEquals("Oslo", enhet.navn)
    }

    @Test
    fun `hent vedtaksperiode med vedtaksperiodeId`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/$vedtaksperiodeId").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val personForSpeilDto = runBlocking { response.receive<PersonForSpeilDto>() }
        assertEquals(
            vedtaksperiodeId.toString(),
            personForSpeilDto.arbeidsgivere.first().vedtaksperioder.first()["id"].asText()
        )
    }

    @Test
    fun `hent vedtaksperiode med aktørId`() {
        val spleisbehovId = UUID.randomUUID()
        val aktørId = "98765"
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = aktørId,
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/aktorId/$aktørId").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val personForSpeilDto = runBlocking { response.receive<PersonForSpeilDto>() }
        assertEquals(
            vedtaksperiodeId.toString(),
            personForSpeilDto.arbeidsgivere.first().vedtaksperioder.first()["id"].asText()
        )
    }

    @Test
    fun `hent vedtaksperiode med fødselsnummer`() {
        val spleisbehovId = UUID.randomUUID()
        val fødselsnummer = "42167376532"
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = fødselsnummer,
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, "{}")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking { client.get<HttpStatement>("/api/person/fnr/$fødselsnummer").execute() }
        assertEquals(HttpStatusCode.OK, response.status)
        val personForSpeilDto = runBlocking { response.receive<PersonForSpeilDto>() }
        assertEquals(
            vedtaksperiodeId.toString(),
            personForSpeilDto.arbeidsgivere.first().vedtaksperioder.first()["id"].asText()
        )
    }

    @Test
    fun `godkjenning av vedtaksperiode`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "12345",
            aktørId = "12345",
            organisasjonsnummer = "89123",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, """{"@id": "$spleisbehovId"}""")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        val response = runBlocking {
            client.post<HttpStatement>("/api/vedtak") {
                body = TextContent(
                    objectMapper.writeValueAsString(
                        Godkjenning(
                            spleisbehovId,
                            true,
                            saksbehandlerIdent = saksbehandlerIdent,
                            årsak = null,
                            begrunnelser = null,
                            kommentar = null
                        )
                    ),
                    contentType = ContentType.Application.Json
                )
            }.execute()
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val løsning = 0.until(rapid.inspektør.size)
            .map(rapid.inspektør::message)
            .first { it.hasNonNull("@løsning") }
            .path("@løsning")
        requireNotNull(løsning)
        assertEquals(løsning["Godkjenning"]["godkjent"].asBoolean(), true)
        assertEquals(løsning["Godkjenning"]["saksbehandlerIdent"].asText(), saksbehandlerIdent)
        assertNotNull(løsning["Godkjenning"]["godkjenttidspunkt"].asLocalDateTime())
    }

    @Test
    fun `en vedtaksperiode kan kun godkjennes en gang`() {
        val spleisbehovId = UUID.randomUUID()
        val godkjenningMessage = GodkjenningMessage(
            id = spleisbehovId,
            fødselsnummer = "6745",
            aktørId = "45637",
            organisasjonsnummer = "56783456",
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 31),
            warnings = emptyList()
        )
        spleisbehovMediator.håndter(godkjenningMessage, """{"@id": "$spleisbehovId"}""")
        spleisbehovMediator.håndter(
            spleisbehovId,
            HentEnhetLøsning("1234"),
            hentPersoninfoLøsning(),
            HentInfotrygdutbetalingerLøsning(infotrygdutbetalingerLøsning())
        )
        runBlocking {
            val godkjenning1 = client.post<HttpStatement>("/api/vedtak") {
                body = TextContent(
                    objectMapper.writeValueAsString(
                        Godkjenning(
                            spleisbehovId,
                            true,
                            saksbehandlerIdent = saksbehandlerIdent,
                            årsak = null,
                            begrunnelser = null,
                            kommentar = null
                        )
                    ),
                    contentType = ContentType.Application.Json
                )
            }.execute()
            assertEquals(HttpStatusCode.Created, godkjenning1.status)

            val godkjenning2 = client.post<HttpStatement>("/api/vedtak") {
                body = TextContent(
                    objectMapper.writeValueAsString(
                        Godkjenning(
                            spleisbehovId,
                            true,
                            saksbehandlerIdent = saksbehandlerIdent,
                            årsak = null,
                            begrunnelser = null,
                            kommentar = null
                        )
                    ),
                    contentType = ContentType.Application.Json
                )
            }.execute()
            assertEquals(HttpStatusCode.Conflict, godkjenning2.status)
        }
    }

    @Test
    fun `404 ved gyldig søketekst-input som ikke eksisterer`() {
        val søketekst = 123456789101
        val responseByAktørId = runBlocking { client.get<HttpStatement>("/api/person/aktorId/$søketekst").execute() }
        val responseByFnr = runBlocking { client.get<HttpStatement>("/api/person/fnr/$søketekst").execute() }
        assertEquals(HttpStatusCode.NotFound, responseByAktørId.status)
        assertEquals(HttpStatusCode.NotFound, responseByFnr.status)
    }

    @Test
    fun `400 ved ikke-numerisk søketekst-input`() {
        val søketekst = "12345678a9101"
        val responseByAktørId = runBlocking { client.get<HttpStatement>("/api/person/aktorId/$søketekst").execute() }
        val responseByFnr = runBlocking { client.get<HttpStatement>("/api/person/fnr/$søketekst").execute() }
        assertEquals(HttpStatusCode.BadRequest, responseByAktørId.status)
        assertEquals(HttpStatusCode.BadRequest, responseByFnr.status)
    }
}

private fun hentPersoninfoLøsning(
    fornavn: String = "Test",
    mellomnavn: String? = null,
    etternavn: String = "Testsen",
    fødselsdato: LocalDate = LocalDate.now(),
    kjønn: Kjønn = Kjønn.Mann
) = HentPersoninfoLøsning(fornavn, mellomnavn, etternavn, fødselsdato, kjønn)

private fun infotrygdutbetalingerLøsning(
    fom: LocalDate = LocalDate.of(2020, 1, 1),
    tom: LocalDate = LocalDate.of(2020, 1, 1),
    grad: Int = 100,
    dagsats: Double = 1200.0,
    typetekst: String = "ArbRef",
    orgnr: String = "89123"
) = objectMapper.readTree(
    """
            [
                {
                    "fom": "$fom",
                    "tom": "$tom",
                    "grad": "$grad",
                    "dagsats": $dagsats,
                    "typetekst": "$typetekst",
                    "organisasjonsnummer": "$orgnr"
                }
            ]
        """.trimIndent()
)

