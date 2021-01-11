package no.nav.helse.modell

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.modell.tildeling.TildelingDao
import no.nav.helse.modell.vedtak.VedtakDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

internal class OppgaveTest {
    private companion object {
        private const val OPPGAVETYPE = "SØKNAD"
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
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val oppgaveMediator = OppgaveMediator(oppgaveDao, vedtakDao, tildelingDao, reservasjonDao)

    private val oppgave = Oppgave.søknad(VEDTAKSPERIODE_ID)

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, vedtakDao, tildelingDao)
    }

    @Test
    fun `oppretter ny oppgave`() {
        every { vedtakDao.findVedtak(VEDTAKSPERIODE_ID) } returns VEDTAK
        oppgave.lagre(oppgaveMediator, HENDELSE_ID, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgaveDao.opprettOppgave(COMMAND_CONTEXT_ID, OPPGAVETYPE, VEDTAKREF) }
        verify(exactly = 1) { oppgaveDao.opprettMakstid(any()) }
    }

    @Test
    fun `oppdater oppgave`() {
        val oppgave = Oppgave(OPPGAVE_ID, OPPGAVETYPE, Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
        oppgave.ferdigstill(SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
        oppgave.lagre(oppgaveMediator, HENDELSE_ID, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgaveDao.updateOppgave(OPPGAVE_ID, Oppgavestatus.Ferdigstilt, SAKSBEHANDLERIDENT, SAKSBEHANDLEROID) }
    }

    @Test
    fun `oppdaterer makstid ved tildeling av oppgave`() {
        every { vedtakDao.findVedtak(VEDTAKSPERIODE_ID) } returns VEDTAK
        val (oid, gyldigTil) = Pair(UUID.randomUUID(), LocalDateTime.now())
        oppgave.lagre(oppgaveMediator, HENDELSE_ID, COMMAND_CONTEXT_ID)
        oppgave.tildel(oppgaveMediator, oid, gyldigTil)
        verify(exactly = 1) { oppgaveDao.oppdaterMakstidVedTildeling(any()) }
    }

    @Test
    fun `gjenoppretter oppgave`() {
        val oppgave1 = Oppgave(OPPGAVE_ID, OPPGAVETYPE, Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
        val oppgave2 = Oppgave(OPPGAVE_ID, OPPGAVETYPE, Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
        val oppgave3 = Oppgave(OPPGAVE_ID, OPPGAVETYPE, Oppgavestatus.AvventerSystem, VEDTAKSPERIODE_ID)
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1, oppgave3)
    }

    @Test
    fun `Setter oppgavestatus til INVALIDERT når oppgaven avbrytes`() {
        val oppgave = Oppgave(OPPGAVE_ID, OPPGAVETYPE, Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
        oppgave.avbryt()
        oppgave.lagre(oppgaveMediator, HENDELSE_ID, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgaveDao.updateOppgave(OPPGAVE_ID, Oppgavestatus.Invalidert, null, null) }
    }

    @Test
    fun `Setter oppgavestatus til MakstidOppnådd når oppgaven timer ut`() {
        val oppgave = Oppgave(OPPGAVE_ID, OPPGAVETYPE, Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
        oppgave.makstidOppnådd()
        oppgave.lagre(oppgaveMediator, HENDELSE_ID, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgaveDao.updateOppgave(OPPGAVE_ID, Oppgavestatus.MakstidOppnådd, null, null) }
    }

    @Test
    fun equals() {
        val gjenopptattOppgave = Oppgave(1L, OPPGAVETYPE,Oppgavestatus.AvventerSaksbehandler, VEDTAKSPERIODE_ID)
        val oppgave1 = Oppgave.søknad(VEDTAKSPERIODE_ID)
        val oppgave2 = Oppgave.søknad(VEDTAKSPERIODE_ID)
        val oppgave3 = Oppgave.søknad(UUID.randomUUID())
        val oppgave4 = Oppgave.stikkprøve(VEDTAKSPERIODE_ID)
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
        assertNotEquals(oppgave1, oppgave3)
        assertNotEquals(oppgave1.hashCode(), oppgave3.hashCode())
        assertNotEquals(oppgave1, oppgave4)
        assertNotEquals(oppgave1.hashCode(), oppgave4.hashCode())

        gjenopptattOppgave.ferdigstill(SAKSBEHANDLERIDENT, SAKSBEHANDLEROID)
        assertNotEquals(gjenopptattOppgave.hashCode(), oppgave2.hashCode())
        assertNotEquals(gjenopptattOppgave, oppgave2)
        assertNotEquals(gjenopptattOppgave, oppgave3)
        assertNotEquals(gjenopptattOppgave, oppgave4)

        oppgave2.ferdigstill("ANNEN_SAKSBEHANDLER", UUID.randomUUID())
        assertEquals(oppgave1, oppgave2)
        assertEquals(oppgave1.hashCode(), oppgave2.hashCode())
    }
}
