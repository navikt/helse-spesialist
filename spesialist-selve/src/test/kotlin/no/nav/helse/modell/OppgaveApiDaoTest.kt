package no.nav.helse.modell

import DatabaseIntegrationTest
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestKommandohendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveApiDaoTest : DatabaseIntegrationTest() {
    private companion object {
        private val CONTEXT_ID = UUID.randomUUID()
        private val TESTHENDELSE = TestKommandohendelse(HENDELSE_ID, UUID.randomUUID(), FNR)
    }

    @BeforeEach
    fun setupDaoTest() {
        godkjenningsbehov(TESTHENDELSE.id)
        CommandContext(CONTEXT_ID).opprett(CommandContextDao(dataSource), TESTHENDELSE)
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

    @Test
    fun `lagre behandlingsreferanse`() {
        val oppgaveId = 1L
        val behandlingId = UUID.randomUUID()
        oppgaveApiDao.lagreBehandlingsreferanse(oppgaveId, behandlingId)
        assertOppgaveBehandlingKobling(oppgaveId, behandlingId)
    }

    private fun assertOppgaveBehandlingKobling(oppgaveId: Long, forventetBehandlingId: UUID) {
        @Language("PostgreSQL")
        val query =
            "SELECT behandling_id FROM oppgave_behandling_kobling obk WHERE obk.oppgave_id = ?"
        val behandlingId = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, oppgaveId).map { it.uuid("behandling_id") }.asSingle)
        }

        assertEquals(forventetBehandlingId, behandlingId)
    }
}
