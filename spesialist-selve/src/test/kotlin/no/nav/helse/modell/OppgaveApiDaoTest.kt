package no.nav.helse.modell

import DatabaseIntegrationTest
import java.sql.SQLException
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestHendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OppgaveApiDaoTest : DatabaseIntegrationTest() {
    private companion object {
        private val CONTEXT_ID = UUID.randomUUID()
        private val TESTHENDELSE = TestHendelse(HENDELSE_ID, UUID.randomUUID(), FNR)
    }

    @BeforeEach
    fun setupDaoTest() {
        godkjenningsbehov(TESTHENDELSE.id)
        CommandContext(CONTEXT_ID).opprett(CommandContextDao(dataSource), TESTHENDELSE)
    }

    @Test
    fun `finner oppgavetype`() {
        nyPerson()
        val type = oppgaveApiDao.finnOppgavetype(VEDTAKSPERIODE)
        assertEquals(OPPGAVETYPE, type.toString())
    }

    @Test
    fun `finner oppgavetype når det fins flere oppgaver for en vedtaksperiode`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId = oppgaveId, oppgavestatus = "Invalidert", egenskaper = listOf(OPPGAVETYPE))
        opprettOppgave(utbetalingId = UUID.randomUUID())

        val type = oppgaveApiDao.finnOppgavetype(VEDTAKSPERIODE)
        assertEquals(OPPGAVETYPE, type.toString())
    }

    @Test
    fun `invaliderer oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        val oppgaveId1 = oppgaveId

        oppgaveApiDao.invaliderOppgaveFor(fødselsnummer = FNR)

        assertOppgaveStatus(oppgaveId1, "Invalidert")
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
    fun `Får feil dersom det finnes flere oppgaver som avventer saksbehandler for en person`() {
        nyPerson()
        opprettOppgave()
        assertThrows<SQLException> {
            oppgaveApiDao.finnOppgaveId(VEDTAKSPERIODE)
        }
    }

    @Test
    fun `Finner oppgave basert på fødselsnummer`() {
        nyPerson()
        val oppgaveId = oppgaveApiDao.finnOppgaveId(FNR)
        assertNotNull(oppgaveId)
    }

    @Test
    fun `Feiler på oppslag på oppgave om det fins flere oppgaver for personen`() {
        nyPerson()
        opprettOppgave()
        assertThrows<SQLException> {
            oppgaveApiDao.finnOppgaveId(FNR)
        }
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

    private fun assertOppgaveStatus(oppgaveId: Long, forventetStatus: String) {
        val status = sessionOf(dataSource).use { session ->
            session.run(
                queryOf("SELECT * FROM oppgave where id = :id", mapOf("id" to oppgaveId))
                    .map { it.string("status") }.asSingle
            )
        }
        assertEquals(forventetStatus, status)
    }
}
