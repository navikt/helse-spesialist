package no.nav.helse.modell

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Oppgavestatus
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.vedtak.VedtakDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random

internal class OppgaveTest {
    private companion object {
        private const val OPPGAVENAVN = "Utbetalingsgodkjenning"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
        private val COMMAND_CONTEXT_ID = UUID.randomUUID()
        private const val SAKSBEHANDLERIDENT = "Z999999"
        private val SAKSBEHANDLEROID = UUID.randomUUID()
        private val OPPGAVE_ID = Random.nextLong()
        private val VEDTAKREF = Random.nextLong()
        private val VEDTAK = VedtakDto(VEDTAKREF, 1L)
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>()
    private val oppgaveMediator = OppgaveMediator(oppgaveDao, vedtakDao)

    private val oppgave = Oppgave.avventerSaksbehandler(OPPGAVENAVN, VEDTAKSPERIODE_ID)

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, vedtakDao)
    }

    @Test
    fun `oppretter ny oppgave`() {
        every { vedtakDao.findVedtak(VEDTAKSPERIODE_ID) } returns VEDTAK
        oppgave.lagre(oppgaveMediator, HENDELSE_ID, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgaveDao.insertOppgave(HENDELSE_ID, COMMAND_CONTEXT_ID, OPPGAVENAVN, Oppgavestatus.AvventerSaksbehandler, null, null, VEDTAKREF) }
    }

    @Test
    fun `oppdater oppgave`() {
        oppgave.ferdigstill(OPPGAVE_ID, SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
        oppgave.lagre(oppgaveMediator, HENDELSE_ID, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgaveDao.updateOppgave(OPPGAVE_ID, Oppgavestatus.Ferdigstilt, SAKSBEHANDLERIDENT, SAKSBEHANDLEROID) }
    }

    @Test
    fun equals() {
        val oppgave1 = Oppgave.avventerSaksbehandler(OPPGAVENAVN, VEDTAKSPERIODE_ID)
        val oppgave2 = Oppgave.avventerSaksbehandler(OPPGAVENAVN, VEDTAKSPERIODE_ID)
        val oppgave3 = Oppgave.avventerSaksbehandler(OPPGAVENAVN, UUID.randomUUID())
        val oppgave4 = Oppgave.avventerSaksbehandler("ET_NAVN", VEDTAKSPERIODE_ID)
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
        assertNotEquals(oppgave1, oppgave3)
        assertNotEquals(oppgave1.hashCode(), oppgave3.hashCode())
        assertNotEquals(oppgave1, oppgave4)
        assertNotEquals(oppgave1.hashCode(), oppgave4.hashCode())

        oppgave1.ferdigstill(OPPGAVE_ID, SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
        assertNotEquals(oppgave1.hashCode(), oppgave2.hashCode())
        assertNotEquals(oppgave1, oppgave2)
        assertNotEquals(oppgave1, oppgave3)
        assertNotEquals(oppgave1, oppgave4)

        oppgave2.ferdigstill(OPPGAVE_ID, "ANNEN_SAKSBEHANDLER", UUID.randomUUID())
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
    }
}
