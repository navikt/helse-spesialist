package no.nav.helse.db

import DatabaseIntegrationTest
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.oppgave.Oppgavemelder
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveDaoTest : DatabaseIntegrationTest() {
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
    fun `Henter oppgavemelding`() {
        val contextId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettGenerasjon()
        opprettVedtaksperiode()
        opprettOppgave(oppgavetype = "SØKNAD", hendelseId = hendelseId, contextId = contextId)
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)

        val oppgaveId = oppgaveDao.finnOppgaveId(UTBETALING_ID)
        val oppgavemelding = oppgaveDao.hentOppgavemelding(oppgaveId!!)
        val forventetOppgavemleding = Oppgavemelder.Oppgavemelding(
            hendelseId = hendelseId,
            oppgaveId = oppgaveId,
            status = "AvventerSaksbehandler",
            type = Oppgavetype.SØKNAD.toString(),
            beslutter = null,
            erRetur = false,
            ferdigstiltAvIdent = null,
            ferdigstiltAvOid = null,
            påVent = false
        )

        assertEquals(forventetOppgavemleding, oppgavemelding)
    }

    @Test
    fun `Finn neste ledige id`() {
        val id1 = oppgaveDao.reserverNesteId()
        val id2 = oppgaveDao.reserverNesteId()
        assertEquals(1L, id1)
        assertEquals(2L, id2)
    }

    @Test
    fun `lagre oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            OPPGAVETYPE,
            listOf(OPPGAVETYPE),
            OPPGAVESTATUS,
            null,
            null,
            vedtakId,
            CONTEXT_ID
        )
    }

    @Test
    fun `lagre oppgave med flere egenskaper`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID, egenskaper = listOf(OPPGAVETYPE, "RISK_QA"))
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            OPPGAVETYPE,
            listOf(OPPGAVETYPE, "RISK_QA"),
            OPPGAVESTATUS,
            null,
            null,
            vedtakId,
            CONTEXT_ID
        )
    }

    @Test
    fun `lagre oppgave med fortrolig adressebeskyttelse`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID, oppgavetype = "FORTROLIG_ADRESSE", egenskaper = listOf("FORTROLIG_ADRESSE"))
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            "FORTROLIG_ADRESSE",
            listOf("FORTROLIG_ADRESSE"),
            OPPGAVESTATUS,
            null,
            null,
            vedtakId,
            CONTEXT_ID
        )
    }

    @Test
    fun `finner utbetalingId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(utbetalingId = UTBETALING_ID)
        assertEquals(UTBETALING_ID, oppgaveDao.finnUtbetalingId(oppgaveId))
    }

    @Test
    fun `finner hendelseId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val contextId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        opprettOppgave(contextId = contextId, hendelseId = hendelseId)
        assertEquals(hendelseId, oppgaveDao.finnHendelseId(oppgaveId))
    }

    @Test
    fun `finner oppgaveId ved hjelp av fødselsnummer`() {
        nyPerson()
        assertEquals(oppgaveId, oppgaveDao.finnOppgaveId(FNR))
    }

    @Test
    fun `finner nyeste oppgaveId uavhengig av status ved hjelp av vedtaksperiodeId`() {
        nyPerson()
        opprettOppgave()
        oppgaveDao.invaliderOppgaveFor(fødselsnummer = FNR)

        val actual = oppgaveDao.finnNyesteOppgaveId(VEDTAKSPERIODE)
        assertEquals(OPPGAVE_ID, actual)
    }

    @Test
    fun `finner OppgaveFraDatabase`() {
        val hendelseId = UUID.randomUUID()
        nyPerson(hendelseId = hendelseId)
        val oppgave = oppgaveDao.finnOppgave(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(
            OppgaveFraDatabase(
                id = oppgaveId,
                egenskap = OPPGAVETYPE,
                egenskaper = listOf(OPPGAVETYPE),
                status = "AvventerSaksbehandler",
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                hendelseId = hendelseId
            ), oppgave
        )
    }

    @Test
    fun `finner OppgaveFraDatabase med flere egenskaper`() {
        val hendelseId = UUID.randomUUID()
        nyPerson(hendelseId = hendelseId, oppgaveEgenskaper = listOf(OPPGAVETYPE, "RISK_QA"))
        val oppgave = oppgaveDao.finnOppgave(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(
            OppgaveFraDatabase(
                id = oppgaveId,
                egenskap = OPPGAVETYPE,
                egenskaper = listOf(OPPGAVETYPE, "RISK_QA"),
                status = "AvventerSaksbehandler",
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                hendelseId = hendelseId
            ), oppgave
        )
    }

    @Test
    fun `finner vedtaksperiodeId`() {
        nyPerson()
        val actual = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
        assertEquals(VEDTAKSPERIODE, actual)
    }

    @Test
    fun `oppdatere oppgave`() {
        val nyStatus = "Ferdigstilt"
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)
        oppgaveDao.updateOppgave(oppgaveId, nyStatus, SAKSBEHANDLEREPOST, SAKSBEHANDLER_OID, listOf(OPPGAVETYPE, "RISK_QA"))
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            OPPGAVETYPE,
            listOf(OPPGAVETYPE, "RISK_QA"),
            nyStatus,
            SAKSBEHANDLEREPOST,
            SAKSBEHANDLER_OID,
            vedtakId,
            CONTEXT_ID
        )
    }

    @Test
    fun `sjekker om det fins aktiv oppgave`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, "AvventerSaksbehandler", null, null, listOf(OPPGAVETYPE))
        assertTrue(oppgaveDao.venterPåSaksbehandler(oppgaveId))

        oppgaveDao.updateOppgave(oppgaveId, "Ferdigstilt", null, null, listOf(OPPGAVETYPE))
        assertFalse(oppgaveDao.venterPåSaksbehandler(oppgaveId))
    }

    @Test
    fun `sjekker om det fins aktiv oppgave med to oppgaver`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId = oppgaveId, oppgavestatus = "AvventerSaksbehandler", egenskaper = listOf(OPPGAVETYPE))

        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)
        oppgaveDao.updateOppgave(oppgaveId = oppgaveId, oppgavestatus = "AvventerSaksbehandler", egenskaper = listOf(OPPGAVETYPE))

        assertTrue(oppgaveDao.harGyldigOppgave(UTBETALING_ID))
    }

    @Test
    fun `sjekker at det ikke fins ferdigstilt oppgave`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId = oppgaveId, oppgavestatus = "AvventerSaksbehandler", egenskaper = listOf(OPPGAVETYPE))

        assertFalse(oppgaveDao.harFerdigstiltOppgave(VEDTAKSPERIODE))
    }

    @Test
    fun `sjekker at det fins ferdigstilt oppgave`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId = oppgaveId, oppgavestatus = "Ferdigstilt", egenskaper = listOf(OPPGAVETYPE))
        oppgaveDao.updateOppgave(oppgaveId = 2L, oppgavestatus = "AvventerSaksbehandler", egenskaper = listOf(OPPGAVETYPE))

        assertTrue(oppgaveDao.harFerdigstiltOppgave(VEDTAKSPERIODE))
    }

    @Test
    fun `henter fødselsnummeret til personen en oppgave gjelder for`() {
        nyPerson()
        val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)
        assertEquals(fødselsnummer, FNR)
    }

    @Test
    fun `oppretter oppgaver med riktig oppgavetype for alle oppgavetype-verdier`() {
        Oppgavetype.entries.forEach {
            assertDoesNotThrow({
                insertOppgave(
                    commandContextId = UUID.randomUUID(),
                    oppgavetype = it,
                    utbetalingId = UUID.randomUUID(),
                )
            }, "Oppgavetype-enumen mangler verdien $it. Kjør migrering: ALTER TYPE oppgavetype ADD VALUE '$it';")
        }
    }

    @Test
    fun `Finner vedtaksperiodeId med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)

        assertEquals(VEDTAKSPERIODE, oppgaveDao.finnVedtaksperiodeId(oppgaveId))
    }

    @Test
    fun `sjekker risk-oppgaver`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID, oppgavetype = "RISK_QA")

        assertTrue(oppgaveDao.erRiskoppgave(oppgaveId))
    }

    @Test
    fun `invaliderer oppgaver`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        val oppgaveId1 = oppgaveId

        val fnr2 = "12312312312"
        val aktørId2 = "43"
        val vedtaksperiodeId2 = UUID.randomUUID()
        opprettPerson(fødselsnummer = fnr2, aktørId2)
        opprettArbeidsgiver("999111888", "en annen bedrift")
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeId2)
        opprettOppgave(vedtaksperiodeId = vedtaksperiodeId2, utbetalingId = UUID.randomUUID())
        val oppgaveId2 = oppgaveId

        oppgaveDao.invaliderOppgaveFor(fødselsnummer = FNR)

        assertOppgaveStatus(oppgaveId1, "Invalidert")
        assertOppgaveStatus(oppgaveId2, "AvventerSaksbehandler")
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

    private fun oppgave() =
        sessionOf(dataSource).use {
            it.run(queryOf("SELECT * FROM oppgave ORDER BY id DESC").map { row ->
                OppgaveAssertions(
                    oppdatert = row.localDate("oppdatert"),
                    type = row.string("type"),
                    egenskaper = row.array<String>("egenskaper").toList(),
                    status = row.string("status"),
                    ferdigstiltAv = row.stringOrNull("ferdigstilt_av"),
                    ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString),
                    vedtakRef = row.longOrNull("vedtak_ref"),
                    commandContextId = row.stringOrNull("command_context_id")?.let(UUID::fromString)
                )
            }.asList)
        }

    private fun insertOppgave(
        commandContextId: UUID,
        oppgavetype: Oppgavetype,
        vedtakRef: Long? = null,
        utbetalingId: UUID,
        status: String = "AvventerSaksbehandler",
        mottaker: Mottaker? = null,
    ) = requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use {
        it.run(
            queryOf(
                """
                INSERT INTO oppgave(oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id, utbetaling_id, mottaker)
                VALUES (now(), CAST(? as oppgavetype), CAST(? as oppgavestatus), ?, ?, ?, ?, ?, CAST(? as mottakertype));
            """,
                oppgavetype.name,
                status,
                null,
                null,
                vedtakRef,
                commandContextId,
                utbetalingId,
                mottaker?.name
            ).asUpdateAndReturnGeneratedKey
        )
    }) { "Kunne ikke opprette oppgave" }

    private class OppgaveAssertions(
        private val oppdatert: LocalDate,
        private val type: String,
        private val egenskaper: List<String>,
        private val status: String,
        private val ferdigstiltAv: String?,
        private val ferdigstiltAvOid: UUID?,
        private val vedtakRef: Long?,
        private val commandContextId: UUID?
    ) {
        fun assertEquals(
            forventetOppdatert: LocalDate,
            forventetType: String,
            forventetEgenskaper: List<String>,
            forventetStatus: String,
            forventetFerdigstilAv: String?,
            forventetFerdigstilAvOid: UUID?,
            forventetVedtakRef: Long?,
            forventetCommandContextId: UUID
        ) {
            assertEquals(forventetOppdatert, oppdatert)
            assertEquals(forventetType, type)
            assertEquals(forventetEgenskaper, egenskaper)
            assertEquals(forventetStatus, status)
            assertEquals(forventetFerdigstilAv, ferdigstiltAv)
            assertEquals(forventetFerdigstilAvOid, ferdigstiltAvOid)
            assertEquals(forventetVedtakRef, vedtakRef)
            assertEquals(forventetCommandContextId, commandContextId)
        }
    }
}
