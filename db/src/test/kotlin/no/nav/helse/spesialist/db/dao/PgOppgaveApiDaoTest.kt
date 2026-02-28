package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgOppgaveApiDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `Finner oppgave basert på fødselsnummer`() {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val behandling = opprettBehandling(vedtaksperiode)
        val oppgave = opprettOppgave(vedtaksperiode, behandling)
        val oppgaveId = oppgaveApiDao.finnOppgaveId(person.id.value)
        assertNotNull(oppgaveId)
        assertEquals(oppgave.id, oppgaveId)
    }

    @Test
    fun `Finner ikke oppgave basert på fødselsnummer dersom person ikke finnes`() {
        val oppgaveId = oppgaveApiDao.finnOppgaveId(lagFødselsnummer())
        assertNull(oppgaveId)
    }
}
