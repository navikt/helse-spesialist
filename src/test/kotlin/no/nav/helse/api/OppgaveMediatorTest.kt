package no.nav.helse.api

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.Oppgave
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.TestHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class OppgaveMediatorTest() {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val HENDELSE_ID = UUID.randomUUID()
        private val COMMAND_CONTEXT_ID = UUID.randomUUID()
        private val TESTHENDELSE = TestHendelse(HENDELSE_ID, VEDTAKSPERIODE_ID, FNR)
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>()
    private val mediator = OppgaveMediator(oppgaveDao, vedtakDao)
    private val oppgave1 = mockk<Oppgave>(relaxed = true)
    private val oppgave2 = mockk<Oppgave>(relaxed = true)

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, oppgave1, oppgave2)
    }

    @Test
    fun `lagrer oppgaver`() {
        mediator.oppgave(oppgave1)
        mediator.oppgave(oppgave2)
        mediator.lagreOppgaver(TESTHENDELSE, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgave1.lagre(oppgaveDao, vedtakDao, HENDELSE_ID, COMMAND_CONTEXT_ID) }
        verify(exactly = 1) { oppgave2.lagre(oppgaveDao, vedtakDao, HENDELSE_ID, COMMAND_CONTEXT_ID) }
    }

    @Test
    fun `lagrer ikke dobbelt`() {
        mediator.oppgave(oppgave1)
        mediator.oppgave(oppgave2)
        mediator.lagreOppgaver(TESTHENDELSE, COMMAND_CONTEXT_ID)
        mediator.lagreOppgaver(TESTHENDELSE, COMMAND_CONTEXT_ID)
        verify(exactly = 1) { oppgave1.lagre(oppgaveDao, vedtakDao, HENDELSE_ID, COMMAND_CONTEXT_ID) }
        verify(exactly = 1) { oppgave2.lagre(oppgaveDao, vedtakDao, HENDELSE_ID, COMMAND_CONTEXT_ID) }
    }

    @Test
    fun `henter oppgave med fødselsnummer`() {
        mediator.hentOppgave(FNR)
        verify(exactly = 1) { oppgaveDao.hentSaksbehandlerOppgave(FNR) }
    }

    @Test
    fun `henter kun saksbehandleroppgaver`() {
        mediator.hentOppgaver()
        verify(exactly = 1) { oppgaveDao.hentSaksbehandlerOppgaver() }
    }
}
