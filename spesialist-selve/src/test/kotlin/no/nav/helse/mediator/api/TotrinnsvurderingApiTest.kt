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
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.notat.NotatMediator
import no.nav.helse.objectMapper
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.periodehistorikk.PeriodehistorikkType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TotrinnsvurderingApiTest : AbstractApiTest() {

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val notatMediator = mockk<NotatMediator>(relaxed = true)
    private val tildelingMediator = mockk<TildelingMediator>(relaxed = true)

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private val totrinnsvurderingDto = TotrinnsvurderingDto(
        oppgavereferanse = 1L,
        periodeId = UUID.randomUUID()
    )
    private val returDtoUtenNotat = TotrinnsvurderingReturDto(
        oppgavereferanse = 1L,
        periodeId = UUID.randomUUID()
    )
    private val returDtoMedNotat = TotrinnsvurderingReturDto(
        oppgavereferanse = 1L,
        periodeId = UUID.randomUUID(),
        notat = "notat_tekst"
    )


    private val TOTRINNSVURDERING_URL = "/api/totrinnsvurdering"
    private val RETUR_URL = "/api/totrinnsvurdering/retur"

    @BeforeAll
    fun setupTotrinnsvurdering() {
        setupServer {
            totrinnsvurderingApi(oppgaveMediator, periodehistorikkDao, notatMediator, tildelingMediator)
        }
    }

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveMediator, periodehistorikkDao, notatMediator, tildelingMediator)
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
            periodehistorikkDao.lagre(
                PeriodehistorikkType.TOTRINNSVURDERING_TIL_GODKJENNING,
                SAKSBEHANDLER_OID,
                totrinnsvurderingDto.periodeId,
                null
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
                setBody<TotrinnsvurderingReturDto>(objectMapper.valueToTree(returDtoUtenNotat))
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }

        verify(exactly = 1) {
            oppgaveMediator.setBeslutterOppgave(
                oppgaveId = returDtoUtenNotat.oppgavereferanse,
                erBeslutterOppgave = false,
                erReturOppgave = true,
                tidligereSaksbehandlerOid = SAKSBEHANDLER_OID
            )
        }
        verify(exactly = 1) {
            tildelingMediator.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
                returDtoUtenNotat.oppgavereferanse,
                any()
            )
        }
        verify(exactly = 1) { periodehistorikkDao.lagre(PeriodehistorikkType.TOTRINNSVURDERING_RETUR, SAKSBEHANDLER_OID, returDtoUtenNotat.periodeId, null) }
        verify(exactly = 0) { notatMediator.lagreForOppgaveId(any(), any(), any()) }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun returMedNotatOk() {
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
                tidligereSaksbehandlerOid = SAKSBEHANDLER_OID
            )
        }
        verify(exactly = 1) {
            tildelingMediator.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
                returDtoUtenNotat.oppgavereferanse,
                any()
            )
        }
        verify(exactly = 1) {
            notatMediator.lagreForOppgaveId(
                returDtoMedNotat.oppgavereferanse,
                returDtoMedNotat.notat!!,
                SAKSBEHANDLER_OID
            )
        }
        verify(exactly = 1) { periodehistorikkDao.lagre(PeriodehistorikkType.TOTRINNSVURDERING_RETUR, SAKSBEHANDLER_OID, returDtoMedNotat.periodeId, any()) }

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

        verify(exactly = 0) { oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any()) }
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

        verify(exactly = 0) { oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any()) }
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

        verify(exactly = 0) { oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any()) }
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

        verify(exactly = 0) { oppgaveMediator.setBeslutterOppgave(any(), any(), any(), any()) }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}