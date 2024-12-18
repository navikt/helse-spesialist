package no.nav.helse.db

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class PgTotrinnsvurderingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `opprett totrinnsvurdering`() {
        opprettPerson()
        val totrinnsvurderingOld: TotrinnsvurderingOld = totrinnsvurderingDao.opprett(VEDTAKSPERIODE)

        assertEquals(VEDTAKSPERIODE, totrinnsvurderingOld.vedtaksperiodeId)
        assertFalse(totrinnsvurderingOld.erRetur)
        assertNull(totrinnsvurderingOld.saksbehandler)
        assertNull(totrinnsvurderingOld.beslutter)
        assertNull(totrinnsvurderingOld.utbetalingIdRef)
        assertNotNull(totrinnsvurderingOld.opprettet)
        assertNull(totrinnsvurderingOld.oppdatert)
    }

    @Test
    fun `Sett saksbehandler på totrinnsvurdering`() {
        opprettPerson()
        opprettSaksbehandler()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        settSaksbehandler(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurdering?.saksbehandler)
        assertNotNull(totrinnsvurdering?.oppdatert)
    }

    @Test
    fun `Sett beslutter på totrinnsvurdering`() {
        opprettPerson()
        opprettSaksbehandler()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        settBeslutter(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurdering?.beslutter)
        assertNotNull(totrinnsvurdering?.oppdatert)
    }

    @Test
    fun `Sett beslutter på totrinnsvurdering med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        opprettSaksbehandler()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settBeslutter(OPPGAVE_ID, SAKSBEHANDLER_OID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurdering?.beslutter)
        assertNotNull(totrinnsvurdering?.oppdatert)
    }

    @Test
    fun `Sett er_retur true på totrinnsvurdering`() {
        opprettPerson()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        requireNotNull(totrinnsvurdering)
        assertTrue(totrinnsvurdering.erRetur)
        assertNotNull(totrinnsvurdering.oppdatert)
    }

    @Test
    fun `Sett er_retur true på totrinnsvurdering med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        settErRetur(OPPGAVE_ID)

        val totrinnsvurdering = totrinnsvurdering()

        requireNotNull(totrinnsvurdering)
        assertTrue(totrinnsvurdering.erRetur)
        assertNotNull(totrinnsvurdering.oppdatert)
    }

    @Test
    fun `Sett er_retur false på totrinnsvurdering`() {
        opprettPerson()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        settHåndtertRetur(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        requireNotNull(totrinnsvurdering)
        assertFalse(totrinnsvurdering.erRetur)
        assertNotNull(totrinnsvurdering.oppdatert)
    }

    @Test
    fun `Sett er_retur false på totrinnsvurdering med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        settHåndtertRetur(OPPGAVE_ID)

        val totrinnsvurdering = totrinnsvurdering()

        requireNotNull(totrinnsvurdering)
        assertFalse(totrinnsvurdering.erRetur)
        assertNotNull(totrinnsvurdering.oppdatert)
    }

    @Test
    fun `Oppdaterer utbetaling_id_ref på totrinnsvurdering`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        val utbetaling_IdId = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID)
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)

        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.ferdigstill(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(utbetaling_IdId, totrinnsvurdering?.utbetalingIdRef)
        assertNotNull(totrinnsvurdering?.oppdatert)
    }

    @Test
    fun `Velger siste utbetaling_id når man ferdigstiller totrinnsvurdering`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID)
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)

        val utbetalingId2 = UUID.randomUUID()
        val utbetalingIdRef2 = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, utbetalingId2)
        opprettUtbetalingKobling(VEDTAKSPERIODE, utbetalingId2)

        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.ferdigstill(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        requireNotNull(totrinnsvurdering)
        assertEquals(utbetalingIdRef2, totrinnsvurdering.utbetalingIdRef)
        assertNotNull(totrinnsvurdering.oppdatert)
    }

    @Test
    fun `Får ikke endret på totrinnsvurdering når utbetaling_id_ref er satt`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        val utbetaling_IdId = lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID)
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.ferdigstill(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        requireNotNull(totrinnsvurdering)
        assertEquals(VEDTAKSPERIODE, totrinnsvurdering.vedtaksperiodeId)
        assertFalse(totrinnsvurdering.erRetur)
        assertNull(totrinnsvurdering.saksbehandler)
        assertNull(totrinnsvurdering.beslutter)
        assertEquals(utbetaling_IdId, totrinnsvurdering.utbetalingIdRef)

        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        settSaksbehandler(VEDTAKSPERIODE, SAKSBEHANDLER_OID)
        settBeslutter(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurderingFerdigstilt = totrinnsvurdering()

        requireNotNull(totrinnsvurderingFerdigstilt)
        assertFalse(totrinnsvurderingFerdigstilt.erRetur)
        assertNull(totrinnsvurderingFerdigstilt.saksbehandler)
        assertNull(totrinnsvurderingFerdigstilt.beslutter)
        assertEquals(utbetaling_IdId, totrinnsvurderingFerdigstilt.utbetalingIdRef)
    }

    @Test
    fun `Finner aktiv ikke-utbetalt totrinnsvurdering`() {
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        val aktivTotrinnsvurdering = totrinnsvurderingDao.hentAktiv(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(aktivTotrinnsvurdering?.vedtaksperiodeId, totrinnsvurdering?.vedtaksperiodeId)
        assertEquals(aktivTotrinnsvurdering?.erRetur, totrinnsvurdering?.erRetur)
        assertEquals(aktivTotrinnsvurdering?.saksbehandler, totrinnsvurdering?.saksbehandler)
        assertEquals(aktivTotrinnsvurdering?.beslutter, totrinnsvurdering?.beslutter)
        assertEquals(aktivTotrinnsvurdering?.utbetalingIdRef, totrinnsvurdering?.utbetalingIdRef)
        assertEquals(aktivTotrinnsvurdering?.oppdatert, totrinnsvurdering?.oppdatert)
        assertEquals(aktivTotrinnsvurdering?.opprettet, totrinnsvurdering?.opprettet)
    }

    @Test
    fun `Finner aktiv totrinnsvurdering`() {
        val beslutter = UUID.randomUUID()
        val oppdatert = LocalDateTime.now()
        opprettSaksbehandler(beslutter)
        opprettSaksbehandler(SAKSBEHANDLER_OID)
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.oppdater(
            TotrinnsvurderingFraDatabase(
                vedtaksperiodeId = VEDTAKSPERIODE,
                erRetur = false,
                saksbehandler = SAKSBEHANDLER_OID,
                beslutter = beslutter,
                utbetalingId = UTBETALING_ID,
                opprettet = LocalDateTime.now(),
                oppdatert = oppdatert
            )
        )
        val aktivTotrinnsvurdering = totrinnsvurderingDao.hentAktivTotrinnsvurdering(OPPGAVE_ID)

        assertEquals(VEDTAKSPERIODE, aktivTotrinnsvurdering?.vedtaksperiodeId)
        assertEquals(false, aktivTotrinnsvurdering?.erRetur)
        assertEquals(SAKSBEHANDLER_OID, aktivTotrinnsvurdering?.saksbehandler)
        assertEquals(beslutter, aktivTotrinnsvurdering?.beslutter)
        assertEquals(null, aktivTotrinnsvurdering?.utbetalingId)
        assertNotNull(aktivTotrinnsvurdering?.opprettet)
        assertEquals(oppdatert.withNano(0), aktivTotrinnsvurdering?.oppdatert?.withNano(0))
    }

    @Test
    fun `Finner aktiv ikke-utbetalt totrinnsvurdering med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        val aktivTotrinnsvurdering = totrinnsvurderingDao.hentAktiv(OPPGAVE_ID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(aktivTotrinnsvurdering?.vedtaksperiodeId, totrinnsvurdering?.vedtaksperiodeId)
        assertEquals(aktivTotrinnsvurdering?.erRetur, totrinnsvurdering?.erRetur)
        assertEquals(aktivTotrinnsvurdering?.saksbehandler, totrinnsvurdering?.saksbehandler)
        assertEquals(aktivTotrinnsvurdering?.beslutter, totrinnsvurdering?.beslutter)
        assertEquals(aktivTotrinnsvurdering?.utbetalingIdRef, totrinnsvurdering?.utbetalingIdRef)
        assertEquals(aktivTotrinnsvurdering?.oppdatert, totrinnsvurdering?.oppdatert)
        assertEquals(aktivTotrinnsvurdering?.opprettet, totrinnsvurdering?.opprettet)
    }

    @Test
    fun `Kan sette saksbehandler med oppgave id`() {
        opprettPerson()
        opprettSaksbehandler()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        settSaksbehandler(OPPGAVE_ID, SAKSBEHANDLER_OID)

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurderingDao.hentAktiv(VEDTAKSPERIODE)?.saksbehandler)
    }

    @Test
    fun `Håndterer oppdatering av totrinnsvurdering som ikke finnes`() {
        opprettPerson()
        opprettSaksbehandler()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        settSaksbehandler(1L, SAKSBEHANDLER_OID)

        assertNull(totrinnsvurderingDao.hentAktiv(1L))
    }

    private fun totrinnsvurdering(vedtaksperiodeId: UUID = VEDTAKSPERIODE) = sessionOf(dataSource, strict = true).use {
        @Language("postgresql")
        val query = "SELECT * FROM totrinnsvurdering WHERE vedtaksperiode_id = :vedtaksperiodeId"
        it.run(queryOf(query, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map { row ->
            TotrinnsvurderingOld(
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                erRetur = row.boolean("er_retur"),
                saksbehandler = row.uuidOrNull("saksbehandler"),
                beslutter = row.uuidOrNull("beslutter"),
                utbetalingIdRef = row.longOrNull("utbetaling_id_ref"),
                opprettet = row.localDateTime("opprettet"),
                oppdatert = row.localDateTimeOrNull("oppdatert")
            )
        }.asSingle)
    }

    private fun settSaksbehandler(oppgaveId: Long, saksbehandlerOid: UUID) = query(
        """
           UPDATE totrinnsvurdering SET saksbehandler = :saksbehandlerOid, oppdatert = now()
           WHERE vedtaksperiode_id = (
               SELECT ttv.vedtaksperiode_id 
               FROM totrinnsvurdering ttv 
               INNER JOIN vedtak v on ttv.vedtaksperiode_id = v.vedtaksperiode_id
               INNER JOIN oppgave o on v.id = o.vedtak_ref
               WHERE o.id = :oppgaveId
               LIMIT 1
           )
           AND utbetaling_id_ref IS null
        """.trimIndent(), "oppgaveId" to oppgaveId, "saksbehandlerOid" to saksbehandlerOid
    ).execute()

    private fun settBeslutter(vedtaksperiodeId: UUID, saksbehandlerOid: UUID) = query(
        """
           UPDATE totrinnsvurdering SET beslutter = :saksbehandlerOid, oppdatert = now()
           WHERE vedtaksperiode_id = :vedtaksperiodeId
           AND utbetaling_id_ref IS null
        """.trimIndent(), "vedtaksperiodeId" to vedtaksperiodeId, "saksbehandlerOid" to saksbehandlerOid
    ).execute()

    private fun settErRetur(oppgaveId: Long) = query(
        """
           UPDATE totrinnsvurdering SET er_retur = true, oppdatert = now()
           WHERE vedtaksperiode_id = (
               SELECT ttv.vedtaksperiode_id 
               FROM totrinnsvurdering ttv 
               INNER JOIN vedtak v on ttv.vedtaksperiode_id = v.vedtaksperiode_id
               INNER JOIN oppgave o on v.id = o.vedtak_ref
               WHERE o.id = :oppgaveId
               LIMIT 1
           )
           AND utbetaling_id_ref IS null
        """.trimIndent(), "oppgaveId" to oppgaveId).execute()

    private fun settHåndtertRetur(vedtaksperiodeId: UUID) = query(
        """
           UPDATE totrinnsvurdering SET er_retur = false, oppdatert = now()
           WHERE vedtaksperiode_id = :vedtaksperiodeId
           AND utbetaling_id_ref IS null
        """.trimIndent(), "vedtaksperiodeId" to vedtaksperiodeId
    ).execute()


    private fun settHåndtertRetur(oppgaveId: Long) = query(
        """
           UPDATE totrinnsvurdering SET er_retur = false, oppdatert = now()
           WHERE vedtaksperiode_id = (
               SELECT ttv.vedtaksperiode_id 
               FROM totrinnsvurdering ttv 
               INNER JOIN vedtak v on ttv.vedtaksperiode_id = v.vedtaksperiode_id
               INNER JOIN oppgave o on v.id = o.vedtak_ref
               WHERE o.id = :oppgaveId
               LIMIT 1
           )
           AND utbetaling_id_ref IS null
        """.trimIndent(), "oppgaveId" to oppgaveId).execute()
}
