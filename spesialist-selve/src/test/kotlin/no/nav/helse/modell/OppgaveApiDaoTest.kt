package no.nav.helse.modell

import DatabaseIntegrationTest
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContextDao
import no.nav.helse.modell.kommando.TestMelding
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OppgaveApiDaoTest : DatabaseIntegrationTest() {
    private val CONTEXT_ID = UUID.randomUUID()
    private val TESTHENDELSE = TestMelding(HENDELSE_ID, UUID.randomUUID(), FNR)

    @BeforeEach
    fun setupDaoTest() {
        godkjenningsbehov(TESTHENDELSE.id)
        CommandContext(CONTEXT_ID).opprett(CommandContextDao(dataSource), TESTHENDELSE.id)
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
