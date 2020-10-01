package no.nav.helse.modell.command

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.Oppgavestatus.Ferdigstilt
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.command.nyny.TestHendelse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OppgaveDaoTest : AbstractEndToEndTest() {
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
    fun `lagre oppgave`() {
        opprettOppgave(contextId = CONTEXT_ID)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            HENDELSE_ID,
            LocalDate.now(),
            OPPGAVETYPE,
            OPPGAVESTATUS,
            null,
            null,
            null,
            CONTEXT_ID
        )
    }

    @Test
    fun `finner contextId`() {
        opprettOppgave(contextId = CONTEXT_ID)
        assertEquals(CONTEXT_ID, oppgaveDao.finnContextId(oppgaveId))
    }

    @Test
    fun `finner hendelseId`() {
        opprettOppgave(contextId = CONTEXT_ID)
        assertEquals(HENDELSE_ID, oppgaveDao.finnHendelseId(oppgaveId))
    }

    @Test
    fun `finner oppgaveId ved hjelp av fødselsnummer`() {
        nyPerson()
        assertEquals(oppgaveId, oppgaveDao.finnOppgaveId(FNR))
    }

    @Test
    fun `finner oppgaver`() {
        nyPerson()
        val oppgaver = oppgaveDao.finnOppgaver()
        val oppgave = oppgaver.first()
        assertTrue(oppgaver.isNotEmpty())
        assertEquals(oppgaveId, oppgave.oppgavereferanse)
    }

    @Test
    fun `finner oppgaver med tildeling`() {
        nyPerson()
        assertEquals(null, oppgaveDao.finnOppgaver().first().saksbehandlerepost)
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST)
        tildelingDao.opprettTildeling(oppgaveId, SAKSBEHANDLER_OID)
        assertEquals(SAKSBEHANDLEREPOST, oppgaveDao.finnOppgaver().first().saksbehandlerepost)
    }

    @Test
    fun `oppdatere oppgave`() {
        val nyStatus = Ferdigstilt
        opprettOppgave(contextId = CONTEXT_ID)
        oppgaveDao.updateOppgave(oppgaveId, nyStatus, SAKSBEHANDLEREPOST, SAKSBEHANDLER_OID)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            HENDELSE_ID,
            LocalDate.now(),
            OPPGAVETYPE,
            nyStatus,
            SAKSBEHANDLEREPOST,
            SAKSBEHANDLER_OID,
            null,
            CONTEXT_ID
        )
    }

    @Test
    fun `sjekker om det fins aktiv oppgave`() {
        opprettOppgave()
        oppgaveDao.updateOppgave(oppgaveId, AvventerSaksbehandler, null, null)
        assertTrue(oppgaveDao.harAktivOppgave(oppgaveId))

        oppgaveDao.updateOppgave(oppgaveId, Ferdigstilt, null, null)
        assertFalse(oppgaveDao.harAktivOppgave(oppgaveId))
    }

    @Test
    fun `kan invalidere oppgaver`() {
        nyPerson()
        opprettOppgave(vedtakId = vedtakId)
        assertFalse(oppgaveDao.finnOppgaver().isEmpty())

        oppgaveDao.invaliderOppgaver(VEDTAKSPERIODE)
        assertTrue(oppgaveDao.finnOppgaver().isEmpty())
    }

    @Test
    fun `invaliderer ikke ferdigstilte oppgaver`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, Ferdigstilt, SAKSBEHANDLEREPOST, SAKSBEHANDLER_OID)

        oppgaveDao.invaliderOppgaver(VEDTAKSPERIODE)
        assertEquals(Ferdigstilt, statusForOppgave(oppgaveId))
    }

    private fun statusForOppgave(oppgaveId: Long) = using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT status FROM oppgave WHERE id = ?", oppgaveId).map {
                enumValueOf<Oppgavestatus>(it.string(1))
            }.asSingle)
        }

    private fun oppgave() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT * FROM oppgave ORDER BY id DESC").map {
                Oppgave(
                    hendelseId = UUID.fromString(it.string("hendelse_id")),
                    oppdatert = it.localDate("oppdatert"),
                    type = it.string("type"),
                    status = enumValueOf(it.string("status")),
                    ferdigstiltAv = it.stringOrNull("ferdigstilt_av"),
                    ferdigstiltAvOid = it.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString),
                    vedtakRef = it.longOrNull("vedtak_ref"),
                    commandContextId = it.stringOrNull("command_context_id")?.let(UUID::fromString)
                )
            }.asList)
        }

    private class Oppgave(
        private val hendelseId: UUID,
        private val oppdatert: LocalDate,
        private val type: String,
        private val status: Oppgavestatus,
        private val ferdigstiltAv: String?,
        private val ferdigstiltAvOid: UUID?,
        private val vedtakRef: Long?,
        private val commandContextId: UUID?
    ) {
        fun assertEquals(
            forventetHendelseId: UUID,
            forventetOppdatert: LocalDate,
            forventetType: String,
            forventetStatus: Oppgavestatus,
            forventetFerdigstilAv: String?,
            forventetFerdigstilAvOid: UUID?,
            forventetVedtakRef: Long?,
            forventetCommandContextId: UUID
        ) {
            assertEquals(forventetHendelseId, hendelseId)
            assertEquals(forventetOppdatert, oppdatert)
            assertEquals(forventetType, type)
            assertEquals(forventetStatus, status)
            assertEquals(forventetFerdigstilAv, ferdigstiltAv)
            assertEquals(forventetFerdigstilAvOid, ferdigstiltAvOid)
            assertEquals(forventetVedtakRef, vedtakRef)
            assertEquals(forventetCommandContextId, commandContextId)
        }
    }
}
