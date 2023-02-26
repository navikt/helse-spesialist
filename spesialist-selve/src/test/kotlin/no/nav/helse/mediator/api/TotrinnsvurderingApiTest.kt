package no.nav.helse.mediator.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.tildeling.TildelingService
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TotrinnsvurderingApiTest : AbstractApiTest() {

    private val varselRepository = mockk<ApiVarselRepository>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val notatMediator = mockk<NotatMediator>(relaxed = true)
    private val tildelingService = mockk<TildelingService>(relaxed = true)
    private val hendelseMediator = mockk<HendelseMediator>(relaxed = true)

    private val saksbehandler_oid = UUID.randomUUID()

    private val totrinnsvurderingDto = TotrinnsvurderingDto(oppgavereferanse = 1L)
    private val returDtoMedNotat = TotrinnsvurderingReturDto(
        oppgavereferanse = 1L,
        notat = NotatApiDto("notat_tekst", NotatType.Retur)
    )

    private val TOTRINNSVURDERING_URL = "/api/totrinnsvurdering"
    private val RETUR_URL = "/api/totrinnsvurdering/retur"

    @BeforeAll
    fun setupTotrinnsvurdering() {
        setupServer {
            totrinnsvurderingApi(varselRepository, oppgaveMediator, periodehistorikkDao, notatMediator, tildelingService, hendelseMediator)
        }
    }

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveMediator, periodehistorikkDao, notatMediator, tildelingService, hendelseMediator)
    }

    @Test
    fun `Kan ikke gjøres til beslutteroppgave hvis den allerede er beslutteroppgave`() {
        every { oppgaveMediator.erBeslutteroppgave(1L) } returns true
        val response = runBlocking {
            client.preparePost("/api/totrinnsvurdering") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(totrinnsvurderingDto))
                authentication(saksbehandler_oid)
            }.execute()
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `en vedtaksperiode kan godkjennes hvis alle varsler er vurdert`() {
        every { oppgaveMediator.erAktivOppgave(1L) } returns true
        every { oppgaveMediator.erRiskoppgave(1L) } returns false
        every { varselRepository.ikkeVurderteVarslerEkskludertBesluttervarslerFor(1L) } returns 0
        val response = runBlocking {
            client.preparePost("/api/totrinnsvurdering") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(totrinnsvurderingDto))
                authentication(saksbehandler_oid)
            }.execute()
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `en vedtaksperiode kan ikke godkjennes hvis det fins aktive varsler`() {
        every { oppgaveMediator.erAktivOppgave(1L) } returns true
        every { oppgaveMediator.erRiskoppgave(1L) } returns false
        every { varselRepository.ikkeVurderteVarslerEkskludertBesluttervarslerFor(1L) } returns 1
        val response = runBlocking {
            client.preparePost("/api/totrinnsvurdering") {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(totrinnsvurderingDto))
                authentication(saksbehandler_oid)
            }.execute()
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun totrinnsvurderingOk() {
        val response = runBlocking {
            client.preparePost(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody<TotrinnsvurderingDto>(objectMapper.valueToTree(totrinnsvurderingDto))
                authentication(saksbehandler_oid)
            }.execute()
        }

        verify(exactly = 1) {
            oppgaveMediator.setBeslutteroppgave(
                oppgaveId = totrinnsvurderingDto.oppgavereferanse,
                tidligereSaksbehandlerOid = saksbehandler_oid
            )
        }
        verify(exactly = 1) {
            tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
                totrinnsvurderingDto.oppgavereferanse,
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            oppgaveMediator.lagrePeriodehistorikk(
                totrinnsvurderingDto.oppgavereferanse,
                periodehistorikkDao,
                saksbehandler_oid,
                PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }


    @Test
    fun returOk() {
        val response = runBlocking {
            client.preparePost(RETUR_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody<TotrinnsvurderingReturDto>(objectMapper.valueToTree(returDtoMedNotat))
                authentication(saksbehandler_oid)
            }.execute()
        }

        verify(exactly = 1) {
            oppgaveMediator.setReturoppgave(
                oppgaveId = returDtoMedNotat.oppgavereferanse,
                beslutterSaksbehandlerOid = saksbehandler_oid
            )
        }
        verify(exactly = 1) {
            tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
                returDtoMedNotat.oppgavereferanse,
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            notatMediator.lagreForOppgaveId(
                returDtoMedNotat.oppgavereferanse,
                returDtoMedNotat.notat.tekst,
                saksbehandler_oid,
                returDtoMedNotat.notat.type
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun totrinnsvurderingManglerPeriodeId() {
        val response = runBlocking {
            client.preparePost(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString("{oppgavereferanse: 1L}"))
                authentication(saksbehandler_oid)
            }.execute()
        }

        verify(exactly = 0) { oppgaveMediator.setBeslutteroppgave(any(), any()) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun returManglerPeriodeId() {
        val response = runBlocking {
            client.preparePost(RETUR_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString("{oppgavereferanse: 1L}"))
                authentication(saksbehandler_oid)
            }.execute()
        }

        verify(exactly = 0) { oppgaveMediator.setBeslutteroppgave(any(), any()) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun totrinnsvurderingManglerAccessToken() {
        val response = runBlocking {
            client.preparePost(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody<TotrinnsvurderingDto>(objectMapper.valueToTree(totrinnsvurderingDto))
            }.execute()
        }

        verify(exactly = 0) { oppgaveMediator.setBeslutteroppgave(any(), any()) }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun returManglerAccessToken() {
        val response = runBlocking {
            client.preparePost(RETUR_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody<TotrinnsvurderingDto>(objectMapper.valueToTree(totrinnsvurderingDto))
            }.execute()
        }

        verify(exactly = 0) { oppgaveMediator.setBeslutteroppgave(any(), any()) }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
