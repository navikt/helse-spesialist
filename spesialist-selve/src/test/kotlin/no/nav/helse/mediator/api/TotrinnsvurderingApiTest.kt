package no.nav.helse.mediator.api

import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.notat.NotatMediator
import no.nav.helse.notat.NotatType
import no.nav.helse.objectMapper
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.periodehistorikk.PeriodehistorikkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TotrinnsvurderingApiTest : AbstractApiTest() {

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val notatMediator = mockk<NotatMediator>(relaxed = true)
    private val tildelingMediator = mockk<TildelingMediator>(relaxed = true)
    private val hendelseMediator = mockk<HendelseMediator>(relaxed = true)

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

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
            totrinnsvurderingApi(oppgaveMediator, periodehistorikkDao, notatMediator, tildelingMediator, hendelseMediator)
        }
    }

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveMediator, periodehistorikkDao, notatMediator, tildelingMediator, hendelseMediator)
    }

    @Test
    fun totrinnsvurderingOk() {
        val response = runBlocking {
            client.preparePost(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody<TotrinnsvurderingDto>(objectMapper.valueToTree(totrinnsvurderingDto))
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        verify(exactly = 1) {
            oppgaveMediator.setBeslutterOppgave(
                oppgaveId = totrinnsvurderingDto.oppgavereferanse,
                erBeslutterOppgave = true,
                erReturOppgave = false,
                totrinnsvurdering = false,
                tidligereSaksbehandlerOid = SAKSBEHANDLER_OID
            )
        }
        verify(exactly = 1) {
            tildelingMediator.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
                totrinnsvurderingDto.oppgavereferanse,
                any()
            )
        }
        verify(exactly = 1) {
            oppgaveMediator.lagrePeriodehistorikk(
                totrinnsvurderingDto.oppgavereferanse,
                periodehistorikkDao,
                SAKSBEHANDLER_OID,
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
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        verify(exactly = 1) {
            oppgaveMediator.setBeslutterOppgave(
                oppgaveId = returDtoMedNotat.oppgavereferanse,
                erBeslutterOppgave = false,
                erReturOppgave = true,
                totrinnsvurdering = true,
                tidligereSaksbehandlerOid = SAKSBEHANDLER_OID
            )
        }
        verify(exactly = 1) {
            tildelingMediator.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
                returDtoMedNotat.oppgavereferanse,
                any()
            )
        }
        verify(exactly = 1) {
            notatMediator.lagreForOppgaveId(
                returDtoMedNotat.oppgavereferanse,
                returDtoMedNotat.notat.tekst,
                SAKSBEHANDLER_OID,
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
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        verify(exactly = 0) { oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any(), any()) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun returManglerPeriodeId() {
        val response = runBlocking {
            client.preparePost(RETUR_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString("{oppgavereferanse: 1L}"))
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        verify(exactly = 0) { oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any(), any()) }
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

        verify(exactly = 0) { oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any(), any()) }
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

        verify(exactly = 0) { oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any(), any()) }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}