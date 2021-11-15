package no.nav.helse.mediator.api

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
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.AzureAdAppConfig
import no.nav.helse.azureAdAppAuthentication
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.objectMapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.net.ServerSocket
import java.util.*

@TestInstance(PER_CLASS)
internal class VedtaksperiodeApiTest {

    private val hendelseMediator: HendelseMediator = mockk(relaxed = true)
    private val personMediator: PersonMediator = mockk(relaxed = true)
    private val saksbehandlerIdent = "1234"
    private val SAKSBEHANDLER_OID = UUID.randomUUID()
    private val godkjenning = GodkjenningDTO(1L, true, saksbehandlerIdent, null, null, null)
    private val FØDSELSNUMMER = "20046913337"
    private val KODE7_SAKSBEHANDLER_GROUP = "c21e58b6-d838-4cb0-919a-398cd40117e3"

    @Test
    fun `godkjenning av vedtaksperiode OK`() {
        every { personMediator.erAktivOppgave(any()) } returns true

        val response = runBlocking {
            client.post<HttpResponse>("/api/vedtak") {
                contentType(ContentType.Application.Json)
                body = objectMapper.valueToTree(godkjenning)
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `en vedtaksperiode kan kun godkjennes en gang`() {
        every { personMediator.erAktivOppgave(any()) } returns false

        val response = runBlocking {
            client.post<HttpResponse>("/api/vedtak") {
                contentType(ContentType.Application.Json)
                body = objectMapper.valueToTree(godkjenning)
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `en person med fortrolig adresse kan ikke hentes av saksbehandler uten tilgang til kode 7`() {
        every { vedtaksperiodeMediator.byggSpeilSnapshotForFnr(any(), eq(true)) } returns mockk(relaxed = true)
        every { vedtaksperiodeMediator.byggSpeilSnapshotForFnr(any(), eq(false)) } returns null


        val response = runBlocking {
            client.get<HttpResponse>("/api/person/fnr/$FØDSELSNUMMER") {
                contentType(ContentType.Application.Json)
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `en person med fortrolig adresse kan hentes av saksbehandler med tilgang til kode 7`() {
        every { vedtaksperiodeMediator.byggSpeilSnapshotForFnr(any(), eq(false)) } returns null
        every { vedtaksperiodeMediator.byggSpeilSnapshotForFnr(any(), eq(true)) } returns mockk(relaxed = true)

        val response = runBlocking {
            client.get<HttpResponse>("/api/person/fnr/$FØDSELSNUMMER") {
                contentType(ContentType.Application.Json)
                authentication(SAKSBEHANDLER_OID, listOf(KODE7_SAKSBEHANDLER_GROUP))
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    private fun HttpRequestBuilder.authentication(oid: UUID, groups: Collection<String> = emptyList()) {
        header(
            "Authorization",
            "Bearer ${
                jwtStub.getToken(
                    groups,
                    oid.toString(),
                    "epostadresse",
                    clientId,
                    issuer
                )
            }"
        )
    }

    private lateinit var server: ApplicationEngine

    private val httpPort = ServerSocket(0).use { it.localPort }
    private val jwtStub = JwtStub()
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
    fun setup() {

        server = embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }

            val jwkProvider = jwtStub.getJwkProviderMock()
            val azureConfig = AzureAdAppConfig(
                clientId = clientId,
                issuer = issuer,
                jwkProvider = jwkProvider
            )
            azureAdAppAuthentication(azureConfig)
            routing {
                authenticate("oidc") {
                    personApi(personMediator, hendelseMediator, KODE7_SAKSBEHANDLER_GROUP)
                }
            }
        }.also {
            it.start(wait = false)
        }
    }

    @AfterAll
    fun tearDown() {
        server.stop(0, 0)
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(personMediator, hendelseMediator)
    }

}
