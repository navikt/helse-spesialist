package no.nav.helse.db

import DatabaseIntegrationTest
import java.time.LocalDate
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.graphql.schema.Mottaker
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Invalidert
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
        opprettOppgave(contextId = CONTEXT_ID, oppgavetype = Oppgavetype.FORTROLIG_ADRESSE)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            Oppgavetype.FORTROLIG_ADRESSE,
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
        opprettOppgave(contextId = CONTEXT_ID)
        assertEquals(HENDELSE_ID, oppgaveDao.finnHendelseId(oppgaveId))
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
    fun `finner oppgave`() {
        nyPerson()
        val oppgave = oppgaveDao.finn(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(
            Oppgave(
                oppgaveId,
                OPPGAVETYPE,
                Oppgave.AvventerSaksbehandler,
                VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID
            ), oppgave
        )
    }

    @Test
    fun `finner OppgaveFraDatabase`() {
        nyPerson()
        val oppgave = oppgaveDao.finnOppgave(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(
            OppgaveFraDatabase(
                id = oppgaveId,
                type = OPPGAVETYPE.toString(),
                status = AvventerSaksbehandler.toString(),
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID
            ), oppgave
        )
    }

    @Test
    fun `finner oppgave fra utbetalingId`() {
        val utbetalingId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            inntektskilde = Inntektskilde.EN_ARBEIDSGIVER
        )
        val oppgaveId = insertOppgave(
            utbetalingId = utbetalingId,
            commandContextId = CONTEXT_ID,
            vedtakRef = vedtakId,
            oppgavetype = OPPGAVETYPE,
            mottaker = Mottaker.ARBEIDSGIVER
        )
        val oppgave = oppgaveDao.finn(utbetalingId) ?: fail { "Fant ikke oppgave" }
        assertEquals(
            Oppgave(
                oppgaveId,
                OPPGAVETYPE,
                Oppgave.AvventerSaksbehandler,
                VEDTAKSPERIODE,
                utbetalingId = utbetalingId
            ), oppgave
        )
    }

    @Test
    fun `finner ikke oppgave fra utbetalingId dersom oppgaven er invalidert`() {
        val utbetalingId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            inntektskilde = Inntektskilde.EN_ARBEIDSGIVER
        )
        insertOppgave(
            utbetalingId = utbetalingId,
            commandContextId = CONTEXT_ID,
            vedtakRef = vedtakId,
            oppgavetype = OPPGAVETYPE,
            status = Invalidert
        )
        val oppgave = oppgaveDao.finn(utbetalingId)
        assertNull(oppgave)
    }

    @Test
    fun `finner vedtaksperiodeId`() {
        nyPerson()
        val actual = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
        assertEquals(VEDTAKSPERIODE, actual)
    }

    @Test
    fun `oppdatere oppgave`() {
        val nyStatus = Ferdigstilt
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)
        oppgaveDao.updateOppgave(oppgaveId, nyStatus, SAKSBEHANDLEREPOST, SAKSBEHANDLER_OID)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            OPPGAVETYPE,
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
        oppgaveDao.updateOppgave(oppgaveId, AvventerSaksbehandler, null, null)
        assertTrue(oppgaveDao.venterPåSaksbehandler(oppgaveId))

        oppgaveDao.updateOppgave(oppgaveId, Ferdigstilt, null, null)
        assertFalse(oppgaveDao.venterPåSaksbehandler(oppgaveId))
    }

    @Test
    fun `sjekker om det fins aktiv oppgave med to oppgaver`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, AvventerSaksbehandler)

        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)
        oppgaveDao.updateOppgave(oppgaveId, AvventerSaksbehandler)

        assertTrue(oppgaveDao.harGyldigOppgave(UTBETALING_ID))
    }

    @Test
    fun `sjekker at det ikke fins ferdigstilt oppgave`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, AvventerSaksbehandler)

        assertFalse(oppgaveDao.harFerdigstiltOppgave(VEDTAKSPERIODE))
    }

    @Test
    fun `sjekker at det fins ferdigstilt oppgave`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, Ferdigstilt)
        oppgaveDao.updateOppgave(2L, AvventerSaksbehandler)

        assertTrue(oppgaveDao.harFerdigstiltOppgave(VEDTAKSPERIODE))
    }


    @Test
    fun `finner oppgave knyttet til vedtaksperiodeId`() {
        nyPerson()
        assertNotNull(oppgaveDao.finnAktiv(VEDTAKSPERIODE))
    }

    @Test
    fun `finner ikke oppgaver for andre vedtaksperiodeider`() {
        nyPerson()
        assertNotNull(oppgaveDao.finnAktiv(VEDTAKSPERIODE))
        oppgaveDao.updateOppgave(oppgaveId, Ferdigstilt)
        assertNull(oppgaveDao.finnAktiv(VEDTAKSPERIODE))

        val vId2 = UUID.randomUUID()
        opprettVedtaksperiode(vId2)
        opprettOppgave(vedtaksperiodeId = vId2)
        assertNull(oppgaveDao.finnAktiv(VEDTAKSPERIODE))
        assertNotNull(oppgaveDao.finnAktiv(vId2))
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
                    utbetalingId = null
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
        opprettOppgave(contextId = CONTEXT_ID, oppgavetype = Oppgavetype.RISK_QA)

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
            it.run(queryOf("SELECT * FROM oppgave ORDER BY id DESC").map {
                OppgaveAssertions(
                    oppdatert = it.localDate("oppdatert"),
                    type = enumValueOf(it.string("type")),
                    status = enumValueOf(it.string("status")),
                    ferdigstiltAv = it.stringOrNull("ferdigstilt_av"),
                    ferdigstiltAvOid = it.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString),
                    vedtakRef = it.longOrNull("vedtak_ref"),
                    commandContextId = it.stringOrNull("command_context_id")?.let(UUID::fromString)
                )
            }.asList)
        }

    private fun insertOppgave(
        commandContextId: UUID,
        oppgavetype: Oppgavetype,
        vedtakRef: Long? = null,
        utbetalingId: UUID?,
        status: Oppgavestatus = AvventerSaksbehandler,
        mottaker: Mottaker? = null,
    ) = requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use {
        it.run(
            queryOf(
                """
                INSERT INTO oppgave(oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id, utbetaling_id, mottaker)
                VALUES (now(), CAST(? as oppgavetype), CAST(? as oppgavestatus), ?, ?, ?, ?, ?, CAST(? as mottakertype));
            """,
                oppgavetype.name,
                status.name,
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
        private val type: Oppgavetype,
        private val status: Oppgavestatus,
        private val ferdigstiltAv: String?,
        private val ferdigstiltAvOid: UUID?,
        private val vedtakRef: Long?,
        private val commandContextId: UUID?
    ) {
        fun assertEquals(
            forventetOppdatert: LocalDate,
            forventetType: Oppgavetype,
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
