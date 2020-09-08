package no.nav.helse.modell.command

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.command.nyny.TestHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class OppgaveDaoTest : AbstractEndToEndTest() {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private const val OPPGAVETYPE = "EN OPPGAVE"
        private val OPPGAVESTATUS = Oppgavestatus.AvventerSaksbehandler
        private const val FERDIGSTIL_AV = "saksbehandler@nav.no"
        private val FERDIGSTILT_AV_OID = UUID.randomUUID()
        private val COMMAND_CONTEXT_ID = UUID.randomUUID()
        private val TESTHENDELSE =
            TestHendelse(HENDELSE_ID, UUID.randomUUID(), "fnr")
    }

    private lateinit var dao: OppgaveDao

    @BeforeEach
    fun setup() {
        dao = OppgaveDao(dataSource)
        testbehov(TESTHENDELSE.id)
        CommandContext(COMMAND_CONTEXT_ID).opprett(CommandContextDao(dataSource), TESTHENDELSE)
    }

    @Test
    fun `lagre oppgave`() {
        dao.insertOppgave(HENDELSE_ID, OPPGAVETYPE, OPPGAVESTATUS, FERDIGSTIL_AV, FERDIGSTILT_AV_OID, null)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            HENDELSE_ID,
            LocalDate.now(),
            OPPGAVETYPE,
            OPPGAVESTATUS,
            FERDIGSTIL_AV,
            FERDIGSTILT_AV_OID,
            null,
            COMMAND_CONTEXT_ID
        )
    }

    @Test
    fun `thrower ved manglende command context`() {
        assertThrows<IllegalArgumentException> {
            dao.insertOppgave(
                UUID.randomUUID(),
                OPPGAVETYPE,
                OPPGAVESTATUS,
                FERDIGSTIL_AV,
                FERDIGSTILT_AV_OID,
                null
            )
        }
    }

    @Test
    fun `oppdatere oppgave`() {
        val nyStatus = Oppgavestatus.Ferdigstilt
        dao.insertOppgave(HENDELSE_ID, OPPGAVETYPE, OPPGAVESTATUS, FERDIGSTIL_AV, FERDIGSTILT_AV_OID, null)
        dao.updateOppgave(HENDELSE_ID, OPPGAVETYPE, nyStatus, null, null)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            HENDELSE_ID,
            LocalDate.now(),
            OPPGAVETYPE,
            nyStatus,
            null,
            null,
            null,
            COMMAND_CONTEXT_ID
        )
    }

    @Test
    fun `oppdatere riktig oppgave`() {
        dao.insertOppgave(HENDELSE_ID, "EN_TYPE", Oppgavestatus.AvventerSaksbehandler, null, null, null)
        dao.insertOppgave(HENDELSE_ID, "EN_ANNEN_TYPE", OPPGAVESTATUS, null, null, null)
        dao.updateOppgave(HENDELSE_ID, "EN_TYPE", Oppgavestatus.Ferdigstilt, FERDIGSTIL_AV, FERDIGSTILT_AV_OID)
        assertEquals(2, oppgave().size)
        oppgave().first().assertEquals(
            HENDELSE_ID,
            LocalDate.now(),
            "EN_ANNEN_TYPE",
            OPPGAVESTATUS,
            null,
            null,
            null,
            COMMAND_CONTEXT_ID
        )
        oppgave().last().assertEquals(
            HENDELSE_ID,
            LocalDate.now(),
            "EN_TYPE",
            Oppgavestatus.Ferdigstilt,
            FERDIGSTIL_AV,
            FERDIGSTILT_AV_OID,
            null,
            COMMAND_CONTEXT_ID
        )
    }

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

    private fun testbehov(hendelseId: UUID) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO spleisbehov(id, data, original, spleis_referanse, type) VALUES(?, ?::json, ?::json, ?, ?)",
                    hendelseId,
                    "{}",
                    "{}",
                    UUID.randomUUID(),
                    "TYPE"
                ).asExecute
            )
        }
    }

}
