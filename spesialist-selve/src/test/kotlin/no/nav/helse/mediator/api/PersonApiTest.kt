package no.nav.helse.mediator.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.auth.authenticate
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.TestApplicationBuilder
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Gruppe
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.idForGruppe
import no.nav.helse.installErrorHandling
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.AzureAdAppConfig
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.testEnv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer

@TestInstance(PER_CLASS)
internal class PersonApiTest {

    private val oppgaveDao: OppgaveDao = mockk(relaxed = true)
    private val totrinnsvurderingMediatorMock = mockk<TotrinnsvurderingMediator>(relaxed = true)
    private val saksbehandlerMediator = mockk<SaksbehandlerMediator>(relaxed = true)
    private val saksbehandlerIdent = "1234"
    private val SAKSBEHANDLER_OID = UUID.randomUUID()
    private val godkjenning = GodkjenningDto(1L, true, saksbehandlerIdent, null, null, null)
    private val avvisning = GodkjenningDto(1L, false, saksbehandlerIdent, "Avvist", null, null)
    private val riskQaGruppe = idForGruppe(Gruppe.RISK_QA)
    private val beslutterGruppe = idForGruppe(Gruppe.BESLUTTER)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @Test
    fun `godkjenning av vedtaksperiode OK`() = iEnTestApplication { client ->
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns null
        val response = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `en vedtaksperiode kan kun godkjennes hvis den har en aktiv oppgave`() = iEnTestApplication { client ->
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns false
        val response = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
        val feilDto = runBlocking { response.body<JsonNode>() }
        assertEquals("ikke_aapen_saksbehandleroppgave", feilDto["feilkode"].asText())
    }

    @Test
    fun `en vedtaksperiode kan godkjennes hvis alle varsler er vurdert`() = iEnTestApplication { client ->
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { oppgaveDao.erRiskoppgave(1L) } returns false
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns null
        every { saksbehandlerMediator.håndter(godkjenning, UUID.randomUUID(), any()) } returns Unit
        val response = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `en vedtaksperiode kan ikke godkjennes hvis det finnes aktive varsler`() = iEnTestApplication { client ->
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { oppgaveDao.erRiskoppgave(1L) } returns false
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns null
        every { saksbehandlerMediator.håndter(godkjenning, any(), any()) } throws ManglerVurderingAvVarsler(1L)
        val response = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `en vedtaksperiode kan avvises selv om det finnes uvurderte varsler`() = iEnTestApplication { client ->
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { oppgaveDao.erRiskoppgave(1L) } returns false
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns null
        every { saksbehandlerMediator.håndter(godkjenning, any(), any()) } returns Unit
        val response = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(avvisning))
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `må ha tilgang for å kunne godkjenne vedtaksperiode med oppgavetype RISK_QA`() = iEnTestApplication { client ->
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { oppgaveDao.erRiskoppgave(1L) } returns true
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns null
        val responseForManglendeTilgang = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID, emptyList())
            }
        }
        assertEquals(HttpStatusCode.Forbidden, responseForManglendeTilgang.status)

        val responseForTilgangOk = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID, listOf(riskQaGruppe))
            }
        }
        assertEquals(HttpStatusCode.Created, responseForTilgangOk.status)
    }

    @Test
    fun `Må ha tilgang til beslutteroppgaver for å kunne godkjenne dem`() = iEnTestApplication { client ->
        val vedtaksperiodeId = UUID.randomUUID()
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { oppgaveDao.erRiskoppgave(1L) } returns false
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns Totrinnsvurdering(
            vedtaksperiodeId = vedtaksperiodeId,
            erRetur = false,
            saksbehandler = UUID.randomUUID(),
            beslutter = null,
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        val responseForManglendeTilgang = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID, emptyList())

            }
        }
        assertEquals(HttpStatusCode.Unauthorized, responseForManglendeTilgang.status)
        val responseBody = runBlocking { responseForManglendeTilgang.body<String>() }
        assertEquals("Saksbehandler trenger beslutter-rolle for å kunne utbetale beslutteroppgaver", responseBody)

        val responseForTilgangOk = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID, listOf(beslutterGruppe))

            }
        }
        assertEquals(HttpStatusCode.Created, responseForTilgangOk.status)
    }

    @Test
    fun `Saksbehandler kan ikke attestere egen beslutteroppgave`() = iEnTestApplication { client ->
        val vedtaksperiodeId = UUID.randomUUID()

        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { oppgaveDao.erRiskoppgave(1L) } returns false
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns Totrinnsvurdering(
            vedtaksperiodeId = vedtaksperiodeId,
            erRetur = false,
            saksbehandler = SAKSBEHANDLER_OID,
            beslutter = null,
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        val responseForManglendeTilgang = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID, listOf(beslutterGruppe))
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, responseForManglendeTilgang.status)
        val responseBody = runBlocking { responseForManglendeTilgang.body<String>() }
        assertEquals("Kan ikke beslutte egne oppgaver.", responseBody)
    }

    @Test
    fun `Setter utbetalende saksbehandlerOid i beslutter-feltet på totrinnsvurdering`() = iEnTestApplication { client ->
        val vedtaksperiodeId = UUID.randomUUID()

        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns Totrinnsvurdering(
            vedtaksperiodeId = vedtaksperiodeId,
            erRetur = false,
            saksbehandler = UUID.randomUUID(),
            beslutter = null,
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        val responseUtbetaling = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID, listOf(beslutterGruppe))
            }
        }

        verify(exactly = 1) { totrinnsvurderingMediatorMock.settBeslutter(vedtaksperiodeId, SAKSBEHANDLER_OID) }
        assertEquals(HttpStatusCode.Created, responseUtbetaling.status)
    }

    private val jwtStub = JwtStub()
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"

    private fun HttpRequestBuilder.authentication(oid: UUID, groups: Collection<String> = emptyList()) {
        header(
            "Authorization",
            "Bearer ${jwtStub.getToken(groups, oid.toString(), "epostadresse", clientId, issuer)}"
        )
    }

    private fun TestApplicationBuilder.setUpApplication() {
        install(ContentNegotiationServer) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
        val azureConfig = AzureConfig(
            clientId = clientId,
            issuer = issuer,
            jwkProvider = jwtStub.getJwkProviderMock(),
            tokenEndpoint = "",
        )

        application {
            installErrorHandling()
            azureAdAppAuthentication(AzureAdAppConfig(azureConfig))
        }
        routing {
            authenticate("oidc") {
                personApi(
                    totrinnsvurderingMediatorMock,
                    mockk(),
                    mockk(relaxed = true),
                    oppgaveDao,
                    Tilgangsgrupper(testEnv),
                    saksbehandlerMediator
                )
            }
        }
    }

    private fun iEnTestApplication(block: ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        setUpApplication()
        val client = createClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }
        block(client)
    }
}
