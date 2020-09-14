package no.nav.helse.modell.command

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.command.nyny.CommandContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OppgaveDaoTest : AbstractEndToEndTest() {

    internal companion object {
        private const val OPPGAVETYPE = "EN OPPGAVE"
        private val OPPGAVESTATUS = Oppgavestatus.AvventerSaksbehandler
        private const val FERDIGSTILT_AV = "saksbehandler@nav.no"
        private val FERDIGSTILT_AV_OID = UUID.randomUUID()
    }

    @BeforeEach
    fun setupDaoTest() {
        testbehov(TESTHENDELSE.id)
        CommandContext(CONTEXT_ID).opprett(CommandContextDao(dataSource), TESTHENDELSE)
    }

    @Test
    fun `lagre oppgave`() {
        nyOppgave()
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            HENDELSE_ID,
            LocalDate.now(),
            OPPGAVETYPE,
            OPPGAVESTATUS,
            FERDIGSTILT_AV,
            FERDIGSTILT_AV_OID,
            null,
            CONTEXT_ID
        )
    }

    @Test
    fun `finner hendelseId på oppgave ved hjelp av fødselsnummer`() {
        nyPerson()
        assertEquals(HENDELSE_ID, oppgaveDao.finnHendelseId(FNR))
    }

    @Test
    fun `finner oppgaver`() {
        nyPerson()
        val oppgaver = oppgaveDao.finnOppgaver()
        assertTrue(oppgaver.isNotEmpty())
    }

    @Test
    fun `finner oppgaver med tildeling`() {
        nyPerson()
        assertEquals(null, oppgaveDao.finnOppgaver().first().saksbehandlerepost)
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST)
        tildelingDao.tildelOppgave(HENDELSE_ID, SAKSBEHANDLER_OID)
        assertEquals(SAKSBEHANDLEREPOST, oppgaveDao.finnOppgaver().first().saksbehandlerepost)
    }

    @Test
    fun `oppdatere oppgave`() {
        val nyStatus = Oppgavestatus.Ferdigstilt
        val id = nyOppgave()
        oppgaveDao.updateOppgave(id, nyStatus, null, null)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            HENDELSE_ID,
            LocalDate.now(),
            OPPGAVETYPE,
            nyStatus,
            null,
            null,
            null,
            CONTEXT_ID
        )
    }

    @Test
    fun `finner contextId`() {
        val id = nyOppgave()
        assertEquals(CONTEXT_ID, oppgaveDao.finnContextId(id))
    }

    @Test
    fun `finner hendelseId`() {
        val id = nyOppgave()
        assertEquals(HENDELSE_ID, oppgaveDao.finnHendelseId(id))
    }

    private fun nyOppgave() = oppgaveDao.insertOppgave(HENDELSE_ID, CONTEXT_ID, OPPGAVETYPE, Oppgavestatus.AvventerSaksbehandler, FERDIGSTILT_AV, FERDIGSTILT_AV_OID, null)

    private fun oppgave() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT * FROM oppgave ORDER BY id DESC").map {
                Oppgave(
                    hendelseId = UUID.fromString(it.string("event_id")),
                    oppdatert = it.localDate("oppdatert"),
                    type = it.string("type"),
                    status = enumValueOf(it.string("status")),
                    ferdigstiltAv = it.stringOrNull("ferdigstilt_av"),
                    ferdigstiltAvOid = it.stringOrNull("ferdigstilt_av_oid")?.let { UUID.fromString(it) },
                    vedtakRef = it.longOrNull("vedtak_ref"),
                    commandContextId = it.stringOrNull("command_context_id")?.let { UUID.fromString(it) }
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
