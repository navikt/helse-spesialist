package no.nav.helse.modell

import DatabaseIntegrationTest
import java.util.UUID
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestMelding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveApiDaoTest : DatabaseIntegrationTest() {
    private val CONTEXT_ID = UUID.randomUUID()
    private val TESTHENDELSE = TestMelding(HENDELSE_ID, UUID.randomUUID(), FNR)

    @BeforeEach
    fun setupDaoTest() {
        godkjenningsbehov(TESTHENDELSE.id)
        CommandContext(CONTEXT_ID).opprett(CommandContextDao(dataSource), TESTHENDELSE.id)
    }

    @Test
    fun `Finner oppgaveId basert på vedtaksperiodeId`() {
        nyPerson()
        val oppgaveId = oppgaveApiDao.finnOppgaveId(VEDTAKSPERIODE)
        assertNotNull(oppgaveId)
        assertEquals(this.oppgaveId, oppgaveId)
    }

    @Test
    fun `Finner ikke oppgaveId basert på vedtaksperiodeId dersom vedtaksperiode ikke finnes`() {
        opprettPerson()
        opprettArbeidsgiver()
        val oppgaveId = oppgaveApiDao.finnOppgaveId(VEDTAKSPERIODE)
        assertNull(oppgaveId)
    }

    @Test
    fun `Finner oppgave basert på fødselsnummer`() {
        nyPerson()
        val oppgaveId = oppgaveApiDao.finnOppgaveId(FNR)
        assertNotNull(oppgaveId)
    }

    @Test
    fun `Finner ikke oppgave basert på fødselsnummer dersom person ikke finnes`() {
        val oppgaveId = oppgaveApiDao.finnOppgaveId(FNR)
        assertNull(oppgaveId)
    }
}
