package no.nav.helse.api

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.TestHendelse
import no.nav.helse.modell.vedtak.VedtakDto
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.tildeling.TildelingDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextLong

internal class OppgaveMediatorTest {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
        private val COMMAND_CONTEXT_ID = UUID.randomUUID()
        private val TESTHENDELSE = TestHendelse(HENDELSE_ID, VEDTAKSPERIODE_ID, FNR)
        private val OPPGAVE_ID = nextLong()
        private val VEDTAKREF = nextLong()
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private const val OPPGAVENAVN1 = "OPPGAVE 1"
        private const val OPPGAVENAVN2 = "OPPGAVE 2"
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val mediator = OppgaveMediator(oppgaveDao, vedtakDao, tildelingDao)
    private lateinit var oppgave1: Oppgave
    private lateinit var oppgave2: Oppgave

    private val testRapid = TestRapid()
    private val messageContext = TestMessageContext(testRapid)

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, vedtakDao, tildelingDao)
        testRapid.reset()
        oppgave1 = Oppgave.avventerSaksbehandler(OPPGAVENAVN1, VEDTAKSPERIODE_ID)
        oppgave2 = Oppgave.avventerSaksbehandler(OPPGAVENAVN2, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `lagrer oppgaver`() {
        every { vedtakDao.findVedtak(VEDTAKSPERIODE_ID) } returns VedtakDto(VEDTAKREF, 2L)
        mediator.oppgave(oppgave1, null)
        mediator.oppgave(oppgave2, null)
        mediator.lagreOppgaver(TESTHENDELSE, messageContext, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgaveDao.opprettOppgave(HENDELSE_ID, COMMAND_CONTEXT_ID, OPPGAVENAVN1, VEDTAKREF) }
        verify(exactly = 1) { oppgaveDao.opprettOppgave(HENDELSE_ID, COMMAND_CONTEXT_ID, OPPGAVENAVN2, VEDTAKREF) }
        assertEquals(2, testRapid.inspektør.size)
        assertOppgaveevent(0, "oppgave_opprettet")
        assertOppgaveevent(1, "oppgave_opprettet")
    }

    @Test
    fun `lagrer tildeling`() {
        val reservasjon = Pair(UUID.randomUUID(), LocalDateTime.now())
        mediator.oppgave(oppgave1, reservasjon)
        mediator.lagreOppgaver(TESTHENDELSE, messageContext, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { tildelingDao.opprettTildeling(any(), reservasjon.first, reservasjon.second) }
    }

    @Test
    fun `oppdaterer oppgave`() {
        oppgave1.ferdigstill(OPPGAVE_ID, SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
        mediator.oppgave(oppgave1, null)
        mediator.lagreOppgaver(TESTHENDELSE, messageContext, COMMAND_CONTEXT_ID)
        assertEquals(1, testRapid.inspektør.size)
        assertOppgaveevent(0, "oppgave_oppdatert", Oppgavestatus.Ferdigstilt) {
            assertEquals(OPPGAVE_ID, it.path("oppgaveId").longValue())
            assertEquals(SAKSBEHANDLERIDENT, it.path("ferdigstiltAvIdent").asText())
            assertEquals(SAKSBEHANDLEROID, UUID.fromString(it.path("ferdigstiltAvOid").asText()))
        }
    }

    @Test
    fun `lagrer ikke dobbelt`() {
        mediator.oppgave(oppgave1, null)
        mediator.oppgave(oppgave2, null)
        mediator.lagreOppgaver(TESTHENDELSE, messageContext, COMMAND_CONTEXT_ID)
        testRapid.reset()
        mediator.lagreOppgaver(TESTHENDELSE, messageContext, COMMAND_CONTEXT_ID)
        assertEquals(0, testRapid.inspektør.size)
        verify(exactly = 1) { oppgaveDao.opprettOppgave(HENDELSE_ID, COMMAND_CONTEXT_ID, OPPGAVENAVN1, any()) }
        verify(exactly = 1) { oppgaveDao.opprettOppgave(HENDELSE_ID, COMMAND_CONTEXT_ID, OPPGAVENAVN2, any()) }
    }

    private fun assertOppgaveevent(indeks: Int, navn: String, status: Oppgavestatus = Oppgavestatus.AvventerSaksbehandler, assertBlock: (JsonNode) -> Unit = {}) {
        testRapid.inspektør.message(indeks).also {
            assertEquals(navn, it.path("@event_name").asText())
            assertEquals(HENDELSE_ID, UUID.fromString(it.path("hendelseId").asText()))
            assertEquals(COMMAND_CONTEXT_ID, UUID.fromString(it.path("contextId").asText()))
            assertEquals(VEDTAKSPERIODE_ID, UUID.fromString(it.path("vedtaksperiodeId").asText()))
            assertEquals(status, enumValueOf<Oppgavestatus>(it.path("status").asText()))
            assertTrue(it.hasNonNull("oppgaveId"))
            assertBlock(it)
        }
    }

    private class TestMessageContext(private val rapidsConnection: RapidsConnection) : RapidsConnection.MessageContext {
        override fun send(message: String) {
            rapidsConnection.publish(message)
        }

        override fun send(key: String, message: String) {
            rapidsConnection.publish(key, message)
        }
    }
}
