package no.nav.helse.mediator.api

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime.now
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.feilhåndtering.ManglerVurderingAvVarsler
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeSendtBeslutter
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.notat.NyttNotatDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.tildeling.TildelingService
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingDto
import no.nav.helse.testEnv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TotrinnsvurderingApiTest : AbstractApiTest() {

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val notatMediator = mockk<NotatMediator>(relaxed = true)
    private val tildelingService = mockk<TildelingService>(relaxed = true)
    private val hendelseMediator = mockk<HendelseMediator>(relaxed = true)
    private val totrinnsvurderingMediator = mockk<TotrinnsvurderingMediator>(relaxed = true)
    private val saksbehandlerMediator = mockk<SaksbehandlerMediator>(relaxed = true)

    private val saksbehandler_oid = UUID.randomUUID()

    private val totrinnsvurderingDto = TotrinnsvurderingDto(oppgavereferanse = 1L)

    private val TOTRINNSVURDERING_URL = "/api/totrinnsvurdering"
    private val RETUR_URL = "/api/totrinnsvurdering/retur"

    private val oppgavehåndterer = object : Oppgavehåndterer {
        var sendtTilBeslutter = false
        var block: () -> Unit = {}
        override fun sendTilBeslutter(oppgaveId: Long, behandlendeSaksbehandler: Saksbehandler) {
            sendtTilBeslutter = true
            block()
        }
    }

    @BeforeAll
    fun setupTotrinnsvurdering() {
        setupServer {
            totrinnsvurderingApi(
                tildelingService,
                hendelseMediator,
                totrinnsvurderingMediator,
                Tilgangsgrupper(testEnv),
                saksbehandlerMediator,
                oppgavehåndterer
            )
        }
    }

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, periodehistorikkDao, notatMediator, tildelingService, hendelseMediator, totrinnsvurderingMediator)
        oppgavehåndterer.sendtTilBeslutter = false
    }

    @Test
    fun `en vedtaksperiode kan godkjennes hvis alle varsler er vurdert`() {
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { oppgaveDao.erRiskoppgave(1L) } returns false
        every { saksbehandlerMediator.håndterTotrinnsvurdering(1L) } returns Unit
        every { totrinnsvurderingMediator.hentAktiv(oppgaveId = any()) } returns TotrinnsvurderingOld(
            vedtaksperiodeId = UUID.randomUUID(),
            erRetur = true,
            saksbehandler = UUID.randomUUID(),
            beslutter = UUID.randomUUID(),
            utbetalingIdRef = null,
            oppdatert = now(),
            opprettet = now()
        )
        val response = runBlocking {
            client.post(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(totrinnsvurderingDto))
                authentication(saksbehandler_oid)
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `en vedtaksperiode kan ikke godkjennes hvis det finnes aktive varsler`() {
        every { oppgaveDao.venterPåSaksbehandler(1L) } returns true
        every { oppgaveDao.erRiskoppgave(1L) } returns false
        every { saksbehandlerMediator.håndterTotrinnsvurdering(1L) } throws ManglerVurderingAvVarsler(1L)
        val response = runBlocking {
            client.post(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(totrinnsvurderingDto))
                authentication(saksbehandler_oid)
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun totrinnsvurderingManglerAccessToken() {
        val response = runBlocking {
            client.post(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(totrinnsvurderingDto)
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun returManglerAccessToken() {
        val response = runBlocking {
            client.post(RETUR_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(totrinnsvurderingDto)
            }
        }

        verify(exactly = 0) { totrinnsvurderingMediator.settRetur(any(), any(), any()) }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `Api gir conflict dersom oppgave allerede er sendt til beslutter`() {
        oppgavehåndterer.block = { throw OppgaveAlleredeSendtBeslutter(1L) }

        val response = runBlocking {
            client.post(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                setBody<JsonNode>(objectMapper.valueToTree(totrinnsvurderingDto))
                authentication(saksbehandler_oid)
            }
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `Oppgave sendes til beslutter når oppgave blir sendt til godkjenning`() {
        val response = runBlocking {
            client.post(TOTRINNSVURDERING_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(TotrinnsvurderingDto(10L))
                authentication(saksbehandler_oid)
            }
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, oppgavehåndterer.sendtTilBeslutter)
    }

    @Test
    fun `Tildeler oppgaven til beslutter dersom den finnes ved returnert retur`() {
    }

    @Test
    fun `Setter totrinnsvurdering til retur false ved returnert retur`() {
    }

    @Test
    fun `Sende totrinnsvurdering i retur`() {
        val tidligereSaksbehandlerOid = UUID.randomUUID()
        val oppgaveId = 2L
        every { totrinnsvurderingMediator.hentAktiv(oppgaveId = oppgaveId) } returns TotrinnsvurderingOld(
            vedtaksperiodeId = UUID.randomUUID(),
            erRetur = true,
            saksbehandler = tidligereSaksbehandlerOid,
            beslutter = null,
            utbetalingIdRef = null,
            oppdatert = now(),
            opprettet = now()
        )

        val notat = "notat_tekst"
        val response = runBlocking {
            client.post(RETUR_URL) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(TotrinnsvurderingReturDto(
                    oppgavereferanse = oppgaveId,
                    notat = NyttNotatDto(notat, NotatType.Retur)
                ))
                authentication(saksbehandler_oid)
            }
        }

        verify(exactly = 1) { totrinnsvurderingMediator.settRetur(oppgaveId, saksbehandler_oid, notat) }

        verify(exactly = 1) {
            tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(
                oppgaveId,
                tidligereSaksbehandlerOid,
                any()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
