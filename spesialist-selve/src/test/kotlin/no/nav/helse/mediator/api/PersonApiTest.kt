package no.nav.helse.mediator.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.ServerSocket
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.AzureAdAppConfig
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer

@TestInstance(PER_CLASS)
internal class PersonApiTest {

    private val varselRepository: ApiVarselRepository = mockk(relaxed = true)
    private val hendelseMediator: HendelseMediator = mockk(relaxed = true)
    private val oppgaveMediator: OppgaveMediator = mockk(relaxed = true)
    private val totrinnsvurderingMediatorMock = mockk<TotrinnsvurderingMediator>(relaxed = true)
    private val saksbehandlerIdent = "1234"
    private val SAKSBEHANDLER_OID = UUID.randomUUID()
    private val godkjenning = GodkjenningDTO(1L, true, saksbehandlerIdent, null, null, null)
    private val avvisning = GodkjenningDTO(1L, false, saksbehandlerIdent, "Avvist", null, null)
    private val riskQaGruppe = UUID.randomUUID()
    private val beslutterGruppe = UUID.randomUUID()

    @Test
    fun `godkjenning av vedtaksperiode OK`() {
        every { oppgaveMediator.erAktivOppgave(1L) } returns true
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
    fun `en vedtaksperiode kan kun godkjennes hvis den har en aktiv oppgave`() {
        every { oppgaveMediator.erAktivOppgave(1L) } returns false
        val response = runBlocking {
            client.post("/api/vedtak") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(godkjenning))
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `en vedtaksperiode kan godkjennes hvis alle varsler er vurdert`() {
        every { oppgaveMediator.erAktivOppgave(1L) } returns true
        every { oppgaveMediator.erRiskoppgave(1L) } returns false
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns null
        every { varselRepository.ikkeVurderteVarslerFor(1L) } returns 0
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
    fun `en vedtaksperiode kan ikke godkjennes hvis det fins aktive varsler`() {
        every { oppgaveMediator.erAktivOppgave(1L) } returns true
        every { oppgaveMediator.erRiskoppgave(1L) } returns false
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns null
        every { varselRepository.ikkeVurderteVarslerFor(1L) } returns 1
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
    fun `en vedtaksperiode kan avvises selv om det finnes uvurderte varsler`() {
        every { oppgaveMediator.erAktivOppgave(1L) } returns true
        every { oppgaveMediator.erRiskoppgave(1L) } returns false
        every { totrinnsvurderingMediatorMock.hentAktiv(1L) } returns null
        every { varselRepository.ikkeVurderteVarslerFor(1L) } returns 1
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
    fun `må ha tilgang for å kunne godkjenne vedtaksperiode med oppgavetype RISK_QA`() {
        every { oppgaveMediator.erAktivOppgave(1L) } returns true
        every { oppgaveMediator.erRiskoppgave(1L) } returns true
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
                authentication(SAKSBEHANDLER_OID, listOf(riskQaGruppe.toString()))

            }
        }
        assertEquals(HttpStatusCode.Created, responseForTilgangOk.status)
    }

    @Test
    fun `Må ha tilgang til beslutteroppgaver for å kunne godkjenne dem`() {
        val vedtaksperiodeId = UUID.randomUUID()
        every { oppgaveMediator.erAktivOppgave(1L) } returns true
        every { oppgaveMediator.erRiskoppgave(1L) } returns false
        every { varselRepository.ikkeVurderteVarslerFor(1L) } returns 0
        every { oppgaveMediator.erBeslutteroppgave(1L) } returns false
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
                authentication(SAKSBEHANDLER_OID, listOf(beslutterGruppe.toString()))

            }
        }
        assertEquals(HttpStatusCode.Created, responseForTilgangOk.status)
    }

    @Test
    fun `Saksbehandler kan ikke attestere egen beslutteroppgave`() {
        val vedtaksperiodeId = UUID.randomUUID()

        every { oppgaveMediator.erAktivOppgave(1L) } returns true
        every { oppgaveMediator.erRiskoppgave(1L) } returns false
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
                authentication(SAKSBEHANDLER_OID, listOf(beslutterGruppe.toString()))
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, responseForManglendeTilgang.status)
        val responseBody = runBlocking { responseForManglendeTilgang.body<String>() }
        assertEquals("Kan ikke beslutte egne oppgaver.", responseBody)
    }

    @Test
    fun `Setter utbetalende saksbehandlerOid i beslutter-feltet på totrinnsvurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()

        every { oppgaveMediator.erAktivOppgave(1L) } returns true
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
                authentication(SAKSBEHANDLER_OID, listOf(beslutterGruppe.toString()))
            }
        }

        verify (exactly = 1) { totrinnsvurderingMediatorMock.settBeslutter(vedtaksperiodeId, SAKSBEHANDLER_OID) }
        assertEquals(HttpStatusCode.Created, responseUtbetaling.status)
    }

//
//    @Test
//    fun `en person med fnr som har fortrolig adresse kan ikke hentes av saksbehandler uten tilgang til kode 7`() {
//        every { personMediator.byggSpeilSnapshotForFnr(any(), eq(true), eq(false)) } returns mockk(relaxed = true)
//        every { personMediator.byggSpeilSnapshotForFnr(any(), eq(false), eq(false)) } returns SnapshotResponse(
//            null,
//            INGEN_TILGANG
//        )
//        val response = runBlocking {
//            client.prepareGet("/api/person/fnr/$FØDSELSNUMMER") {
//                contentType(ContentType.Application.Json)
//                authentication(SAKSBEHANDLER_OID)
//            }.execute()
//        }
//        assertEquals(HttpStatusCode.Forbidden, response.status)
//    }
//
//    @Test
//    fun `en person med aktørId som har fortrolig adresse kan ikke hentes av saksbehandler uten tilgang til kode 7`() {
//        every { personMediator.byggSpeilSnapshotForAktørId(any(), eq(true), eq(false)) } returns mockk(relaxed = true)
//        every { personMediator.byggSpeilSnapshotForAktørId(any(), eq(false), eq(false)) } returns SnapshotResponse(
//            null,
//            INGEN_TILGANG
//        )
//        val response = runBlocking {
//            client.prepareGet("/api/person/aktorId/$AKTØRID") {
//                contentType(ContentType.Application.Json)
//                authentication(SAKSBEHANDLER_OID)
//            }.execute()
//        }
//        assertEquals(HttpStatusCode.Forbidden, response.status)
//    }
//
//    @Test
//    fun `en person med fnr som har fortrolig adresse kan hentes av saksbehandler med tilgang til kode 7`() {
//        every { personMediator.byggSpeilSnapshotForFnr(any(), eq(false), eq(false)) } returns SnapshotResponse(null, INGEN_TILGANG)
//        every { personMediator.byggSpeilSnapshotForFnr(any(), eq(true), eq(false)) } returns mockk(relaxed = true)
//
//        val response = runBlocking {
//            client.prepareGet("/api/person/fnr/$FØDSELSNUMMER") {
//                contentType(ContentType.Application.Json)
//                authentication(SAKSBEHANDLER_OID, listOf(KODE7_SAKSBEHANDLER_GROUP.toString()))
//            }.execute()
//        }
//        assertEquals(HttpStatusCode.OK, response.status)
//    }
//
//    @Test
//    fun `en person med aktørId som har fortrolig adresse kan hentes av saksbehandler med tilgang til kode 7`() {
//        every { personMediator.byggSpeilSnapshotForAktørId(any(), eq(false), eq(false)) } returns SnapshotResponse(null, INGEN_TILGANG)
//        every { personMediator.byggSpeilSnapshotForAktørId(any(), eq(true), eq(false)) } returns mockk(relaxed = true)
//
//        val response = runBlocking {
//            client.prepareGet("/api/person/aktorId/$AKTØRID") {
//                contentType(ContentType.Application.Json)
//                authentication(SAKSBEHANDLER_OID, listOf(KODE7_SAKSBEHANDLER_GROUP.toString()))
//            }.execute()
//        }
//        assertEquals(HttpStatusCode.OK, response.status)
//    }
//
//    @Test
//    fun `en skjermet person kan ikke hentes av saksbehandler som ikke har tilgang til skjermede personer`() {
//        every { personMediator.byggSpeilSnapshotForAktørId(any(), eq(false), eq(false)) } returns SnapshotResponse(null, INGEN_TILGANG)
//        every { personMediator.byggSpeilSnapshotForFnr(any(), eq(false), eq(false)) } returns SnapshotResponse(null, INGEN_TILGANG)
//
//        runBlocking {
//            client.prepareGet("/api/person/aktorId/$AKTØRID") {
//                contentType(ContentType.Application.Json)
//                authentication(SAKSBEHANDLER_OID)
//            }.execute()
//        }.apply {
//            assertEquals(HttpStatusCode.Forbidden, status)
//        }
//
//        runBlocking {
//            client.prepareGet("/api/person/fnr/$FØDSELSNUMMER") {
//                contentType(ContentType.Application.Json)
//                authentication(SAKSBEHANDLER_OID)
//            }.execute()
//        }.apply {
//            assertEquals(HttpStatusCode.Forbidden, status)
//        }
//    }
//
//    @Test
//    fun `en skjermet person kan hentes av saksbehandler med tilgang til skjermede personer`() {
//        every { personMediator.byggSpeilSnapshotForAktørId(any(), eq(false), eq(true)) } returns mockk(relaxed = true)
//        every { personMediator.byggSpeilSnapshotForFnr(any(), eq(false), eq(true)) } returns mockk(relaxed = true)
//
//        runBlocking {
//            client.prepareGet("/api/person/aktorId/$AKTØRID") {
//                contentType(ContentType.Application.Json)
//                authentication(SAKSBEHANDLER_OID, listOf(SKJERMEDE_PERSONER_GROUP.toString()))
//            }.execute()
//        }.apply {
//            assertEquals(HttpStatusCode.OK, status)
//        }
//
//        runBlocking {
//            client.prepareGet("/api/person/fnr/$FØDSELSNUMMER") {
//                contentType(ContentType.Application.Json)
//                authentication(SAKSBEHANDLER_OID, listOf(SKJERMEDE_PERSONER_GROUP.toString()))
//            }.execute()
//        }.apply {
//            assertEquals(HttpStatusCode.OK, status)
//        }
//    }

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
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
    }

    @BeforeAll
    fun setup() {
        server = embeddedServer(CIO, port = httpPort) {
            install(ContentNegotiationServer) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            val azureConfig = AzureConfig(
                clientId = clientId,
                issuer = issuer,
                jwkProvider = jwtStub.getJwkProviderMock(),
                tokenEndpoint = "",
            )
            val azureAdAppConfig = AzureAdAppConfig(
                azureConfig = azureConfig,
            )

            azureAdAppAuthentication(azureAdAppConfig)
            routing {
                authenticate("oidc") {
                    personApi(
                        varselRepository,
                        totrinnsvurderingMediatorMock,
                        hendelseMediator,
                        oppgaveMediator,
                        Tilgangsgrupper(
                            mapOf(
                                Tilgangsgrupper.riskQaKey to riskQaGruppe.toString(),
                                Tilgangsgrupper.beslutterKey to beslutterGruppe.toString()
                            )
                        )
                    )
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
        clearMocks(hendelseMediator)
    }

}
