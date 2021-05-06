package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgavestatus
import no.nav.helse.modell.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.modell.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

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
        val oppgaver = oppgaveDao.finnOppgaver(false)
        val oppgave = oppgaver.first()
        assertTrue(oppgaver.isNotEmpty())
        assertEquals(oppgaveId.toString(), oppgave.oppgavereferanse)
    }

    @Test
    fun `inkluder risk qa oppgaver bare for supersaksbehandlere`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtakId = vedtakId, oppgavetype = "RISK_QA")

        val oppgaver = oppgaveDao.finnOppgaver(true)
        assertTrue(oppgaver.isNotEmpty())
        val oppgave = oppgaver.first()
        assertEquals("RISK_QA", oppgave.oppgavetype)
        assertEquals(oppgaveId.toString(), oppgave.oppgavereferanse)
        assertTrue(oppgaveDao.finnOppgaver(false).isEmpty())
    }

    @Test
    fun `sorterer RISK_QA-oppgaver først, så forlengelser, så resten`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtakId = vedtakId)

        opprettVedtaksperiode(vedtaksperiodeId = UUID.randomUUID(), periodetype = OVERGANG_FRA_IT)
        opprettOppgave(vedtakId = vedtakId, oppgavetype = "RISK_QA")

        opprettVedtaksperiode(vedtaksperiodeId = UUID.randomUUID(), periodetype = INFOTRYGDFORLENGELSE)
        opprettOppgave(vedtakId = vedtakId)

        opprettVedtaksperiode(vedtaksperiodeId = UUID.randomUUID(), periodetype = FORLENGELSE)
        opprettOppgave(vedtakId = vedtakId)

        val oppgaver = oppgaveDao.finnOppgaver(true)
        assertEquals("RISK_QA", oppgaver[0].oppgavetype)
        assertEquals(INFOTRYGDFORLENGELSE, oppgaver[1].type)
        assertEquals(FORLENGELSE, oppgaver[2].type)
        assertEquals(FØRSTEGANGSBEHANDLING, oppgaver[3].type)
    }

    @Test
    fun `finner oppgave`() {
        nyPerson()
        val oppgave = oppgaveDao.finn(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(Oppgave(oppgaveId, OPPGAVETYPE, AvventerSaksbehandler, VEDTAKSPERIODE, utbetalingId = UTBETALING_ID), oppgave)
    }

    @Test
    fun `finner oppgave fra utbetalingId`() {
        val utbetalingId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(periodetype = FØRSTEGANGSBEHANDLING, inntektskilde = Inntektskilde.EN_ARBEIDSGIVER)
        val oppgaveId = insertOppgave(utbetalingId = utbetalingId, commandContextId = CONTEXT_ID, vedtakRef = vedtakId, oppgavetype = OPPGAVETYPE)
        val oppgave = oppgaveDao.finn(utbetalingId) ?: fail { "Fant ikke oppgave" }
        assertEquals(Oppgave(oppgaveId, OPPGAVETYPE, AvventerSaksbehandler, VEDTAKSPERIODE, utbetalingId = utbetalingId), oppgave)
    }

    @Test
    fun `kan hente oppgave selv om utbetalingId mangler`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(periodetype = FØRSTEGANGSBEHANDLING, inntektskilde = Inntektskilde.EN_ARBEIDSGIVER)
        val oppgaveId = insertOppgave(utbetalingId = null, commandContextId = CONTEXT_ID, vedtakRef = vedtakId, oppgavetype = OPPGAVETYPE)
        val oppgave = oppgaveDao.finn(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(Oppgave(oppgaveId, OPPGAVETYPE, AvventerSaksbehandler, VEDTAKSPERIODE, utbetalingId = null), oppgave)
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
        assertEquals(null, oppgaveDao.finnOppgaver(false).first().tildeling?.epost)
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, "Navn Navnesen", SAKSBEHANDLEREPOST)
        tildelingDao.opprettTildeling(oppgaveId, SAKSBEHANDLER_OID)
        assertEquals(SAKSBEHANDLEREPOST, oppgaveDao.finnOppgaver(false).first().tildeling?.epost)
        assertEquals(SAKSBEHANDLEREPOST, oppgaveDao.finnOppgaver(false).first().tildeling?.epost)
        assertEquals(false, oppgaveDao.finnOppgaver(false).first().tildeling?.påVent)
        assertEquals(SAKSBEHANDLER_OID, oppgaveDao.finnOppgaver(false).first().tildeling?.oid)
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
        assertTrue(oppgaveDao.venterPåSaksbehandler(oppgaveId))

        oppgaveDao.updateOppgave(oppgaveId, Ferdigstilt, null, null)
        assertFalse(oppgaveDao.venterPåSaksbehandler(oppgaveId))
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
    fun `sjekker at det ikke fins ferdigstilt oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        val vedtakId = opprettVedtaksperiode()

        opprettOppgave(vedtakId = vedtakId)
        oppgaveDao.updateOppgave(oppgaveId, AvventerSaksbehandler)

        assertFalse(oppgaveDao.harFerdigstiltOppgave(VEDTAKSPERIODE))
    }

    @Test
    fun `sjekker at det fins ferdigstilt oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        val vedtakId = opprettVedtaksperiode()

        opprettOppgave(vedtakId = vedtakId)
        oppgaveDao.updateOppgave(oppgaveId, Ferdigstilt)

        assertTrue(oppgaveDao.harFerdigstiltOppgave(VEDTAKSPERIODE))
    }


    @Test
    fun `finner alle oppgaver knyttet til vedtaksperiodeId`() {
        nyPerson()
        opprettOppgave(vedtakId = vedtakId)
        val oppgaver = oppgaveDao.finnAktive(VEDTAKSPERIODE)
        assertEquals(2, oppgaver.size)
    }

    @Test
    fun `finner ikke oppgaver knyttet til andre vedtaksperiodeider`() {
        nyPerson()
        opprettVedtaksperiode(UUID.randomUUID())
        opprettOppgave(vedtakId = vedtakId)
        assertEquals(1, oppgaveDao.finnAktive(VEDTAKSPERIODE).size)
    }

    @Test
    fun `henter fødselsnummeret til personen en oppgave gjelder for`(){
        nyPerson()
        val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)
        assertEquals(fødselsnummer, FNR)
    }

    @Test
    fun `en oppgave har riktig oppgavetype og inntektskilde`(){
        nyPerson(inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE)
        val oppgaver = oppgaveDao.finnOppgaver(true)
        assertEquals(FØRSTEGANGSBEHANDLING, oppgaver.first().type)
        assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, oppgaver.first().inntektskilde)
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

    private fun insertOppgave(
        commandContextId: UUID,
        oppgavetype: String,
        vedtakRef: Long? = null,
        utbetalingId: UUID?
    ) = requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.run(
            queryOf(
                """
                INSERT INTO oppgave(oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id, utbetaling_id)
                VALUES (now(), CAST(? as oppgavetype), CAST(? as oppgavestatus), ?, ?, ?, ?, ?);
            """,
                oppgavetype,
                AvventerSaksbehandler.name,
                null,
                null,
                vedtakRef,
                commandContextId,
                utbetalingId
            ).asUpdateAndReturnGeneratedKey
        )
    }) { "Kunne ikke opprette oppgave" }

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
