package no.nav.helse.modell.command.nyny

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.VedtakDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SaksbehandlerGodkjenningCommandTest {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val JSON = "{}"
        private const val SAKSBEHANDLER = "Saksbehandler"
        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private const val EPOST = "saksbehandler@nav.no"
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private lateinit var context: CommandContext
    private val command = SaksbehandlerGodkjenningCommand(HENDELSE_ID, VEDTAKSPERIODE_ID, oppgaveDao, vedtakDao, JSON)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(oppgaveDao, vedtakDao)
    }

    @Test
    fun `oppretter oppgave`() {
        val vedtakRef = 1L
        every { vedtakDao.findVedtak(VEDTAKSPERIODE_ID) } returns VedtakDto(vedtakRef, 0L)
        assertFalse(command.execute(context))
        verify(exactly = 1) { oppgaveDao.insertOppgave(HENDELSE_ID, any(), Oppgavestatus.AvventerSaksbehandler, null, null, vedtakRef) }
        verify(exactly = 0) { oppgaveDao.updateOppgave(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `ferdigstiller oppgave`() {
        val vedtakRef = 1L
        every { vedtakDao.findVedtak(VEDTAKSPERIODE_ID) } returns VedtakDto(vedtakRef, 0L)
        context.add(SaksbehandlerLøsning(true, SAKSBEHANDLER, SAKSBEHANDLER_OID, EPOST, GODKJENTTIDSPUNKT, null, null, null))
        assertTrue(command.execute(context))
        assertEquals(1, context.meldinger().size)
        verify(exactly = 1) { oppgaveDao.updateOppgave(HENDELSE_ID, any(), Oppgavestatus.Ferdigstilt, EPOST, SAKSBEHANDLER_OID) }
    }

    @Test
    fun resume() {
        val vedtakRef = 1L
        every { vedtakDao.findVedtak(VEDTAKSPERIODE_ID) } returns VedtakDto(vedtakRef, 0L)
        context.add(SaksbehandlerLøsning(true, SAKSBEHANDLER, SAKSBEHANDLER_OID, EPOST, GODKJENTTIDSPUNKT, null, null, null))
        assertTrue(command.resume(context))
        assertEquals(1, context.meldinger().size)
        verify(exactly = 0) { oppgaveDao.insertOppgave(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { oppgaveDao.updateOppgave(HENDELSE_ID, any(), Oppgavestatus.Ferdigstilt, EPOST, SAKSBEHANDLER_OID) }
    }
}
