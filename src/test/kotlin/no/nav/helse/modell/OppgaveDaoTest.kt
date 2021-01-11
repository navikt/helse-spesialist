package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.modell.Oppgavestatus.Ferdigstilt
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestHendelse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLException
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

internal class OppgaveDaoTest : DatabaseIntegrationTest() {
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
    fun `finner oppgave`() {
        nyPerson()
        val oppgave = oppgaveDao.finn(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(Oppgave(oppgaveId, OPPGAVETYPE, AvventerSaksbehandler, VEDTAKSPERIODE), oppgave)
    }

    @Test
    fun `finner vedtaksperiodeId`() {
        nyPerson()
        val actual = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
        assertEquals(VEDTAKSPERIODE, actual)
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
        assertTrue(oppgaveDao.erAktivOppgave(oppgaveId))

        oppgaveDao.updateOppgave(oppgaveId, Ferdigstilt, null, null)
        assertFalse(oppgaveDao.erAktivOppgave(oppgaveId))
    }

    @Test
    fun `sjekker om det fins aktiv oppgave med to oppgaver`() {
        opprettPerson()
        opprettArbeidsgiver()
        val vedtakId = opprettVedtaksperiode()

        opprettOppgave(vedtakId = vedtakId)
        oppgaveDao.updateOppgave(oppgaveId, AvventerSaksbehandler)

        opprettOppgave(vedtakId = vedtakId)
        oppgaveDao.updateOppgave(oppgaveId, AvventerSaksbehandler)

        assertTrue(oppgaveDao.harAktivOppgave(VEDTAKSPERIODE))
    }

    @Test
    fun `finner alle oppgaver knyttet til vedtaksperiodeId`() {
        nyPerson()
        opprettOppgave(vedtakId = vedtakId)
        val oppgaver = oppgaveDao.finn(VEDTAKSPERIODE)
        assertEquals(2, oppgaver.size)
    }

    @Test
    fun `finner ikke oppgaver knyttet til andre vedtaksperiodeider`() {
        nyPerson()
        opprettVedtaksperiode(UUID.randomUUID())
        opprettOppgave(vedtakId = vedtakId)
        assertEquals(1, oppgaveDao.finn(VEDTAKSPERIODE).size)
    }

    @Test
    fun `opprette makstid på 4 dager end of day for ny oppgave`() {
        opprettOppgave()
        oppgaveDao.opprettMakstid(oppgaveId)
        assertEquals(
            LocalDate
                .now()
                .plusDays(4)
                .atTime(23, 59, 59),
            oppgaveDao.finnMakstid(oppgaveId)
        )
        assertFalse(oppgaveMedMakstidErTildelt(oppgaveId))
    }

    @Test
    fun `kan ikke opprette en oppgave_makstid flere ganger for samme oppgave`() {
        opprettOppgave()
        oppgaveDao.opprettMakstid(oppgaveId)
        assertThrows<PSQLException>{ oppgaveDao.opprettMakstid(oppgaveId) }
    }

    @Test
    fun `oppdaterer makstid i oppgave_makstid for en oppgave når den blir tildelt`() {
        opprettOppgave()
        oppgaveDao.opprettMakstid(oppgaveId)
        oppgaveDao.oppdaterMakstidVedTildeling(oppgaveId)
        assertEquals(
            LocalDate
                .now()
                .plusDays(14)
                .atTime(23, 59, 59),
            oppgaveDao.finnMakstid(oppgaveId)
        )
        assertTrue(oppgaveMedMakstidErTildelt(oppgaveId))
    }

    @Test
    fun `en oppgave får ikke ny makstid hvis den tildeles flere ganger, første makstid for oppgaven er gjeldende`() {
        opprettOppgave()
        oppgaveDao.opprettMakstid(oppgaveId)
        oppgaveDao.oppdaterMakstidVedTildeling(oppgaveId, 14)
        oppgaveDao.oppdaterMakstidVedTildeling(oppgaveId, 2)
        assertEquals(
            LocalDate
                .now()
                .plusDays(14)
                .atTime(23, 59, 59),
            oppgaveDao.finnMakstid(oppgaveId)
        )
    }

    @Test
    fun `henter fødselsnummeret til personen en oppgave gjelder for`(){
        nyPerson()
        val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)
        assertEquals(fødselsnummer, FNR)
    }

    private fun statusForOppgave(oppgaveId: Long) = using(sessionOf(dataSource)) { session ->
        session.run(queryOf("SELECT status FROM oppgave WHERE id = ?", oppgaveId).map {
            enumValueOf<Oppgavestatus>(it.string(1))
        }.asSingle)
    }

    private fun oppgave() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT * FROM oppgave ORDER BY id DESC").map {
                OppgaveAssertions(
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

    private fun oppgaveMedMakstidErTildelt(oppgaveId: Long): Boolean =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT tildelt FROM oppgave_makstid WHERE oppgave_ref=?", oppgaveId).map {
                it.boolean("tildelt")
            }.asSingle) ?: false
        }

    private class OppgaveAssertions(
        private val oppdatert: LocalDate,
        private val type: String,
        private val status: Oppgavestatus,
        private val ferdigstiltAv: String?,
        private val ferdigstiltAvOid: UUID?,
        private val vedtakRef: Long?,
        private val commandContextId: UUID?
    ) {
        fun assertEquals(
            forventetOppdatert: LocalDate,
            forventetType: String,
            forventetStatus: Oppgavestatus,
            forventetFerdigstilAv: String?,
            forventetFerdigstilAvOid: UUID?,
            forventetVedtakRef: Long?,
            forventetCommandContextId: UUID
        ) {
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
