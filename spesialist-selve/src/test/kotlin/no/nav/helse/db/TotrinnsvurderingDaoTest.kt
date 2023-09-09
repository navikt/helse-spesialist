package no.nav.helse.db

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.juli
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingDaoTest : DatabaseIntegrationTest() {

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
        totrinnsvurderingDao.settSaksbehandler(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurdering?.saksbehandler)
        assertNotNull(totrinnsvurdering?.oppdatert)
    }

    @Test
    fun `Sett beslutter på totrinnsvurdering`() {
        opprettPerson()
        opprettSaksbehandler()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settBeslutter(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

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
        totrinnsvurderingDao.settErRetur(OPPGAVE_ID)

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
        totrinnsvurderingDao.settHåndtertRetur(VEDTAKSPERIODE)

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
        totrinnsvurderingDao.settHåndtertRetur(OPPGAVE_ID)

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
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 10.juli(), 12000)
        lagLinje(personOppdragId, 11.juli(), 31.juli(), 10000)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID)
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)

        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.ferdigstill(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(1, totrinnsvurdering?.utbetalingIdRef)
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
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 10.juli(), 12000)
        lagLinje(personOppdragId, 11.juli(), 31.juli(), 10000)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID)
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)

        val utbetalingId2 = UUID.randomUUID()
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 10.juli(), 12000)
        lagLinje(personOppdragId, 11.juli(), 31.juli(), 10000)
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
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 10.juli(), 12000)
        lagLinje(personOppdragId, 11.juli(), 31.juli(), 10000)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID)
        opprettUtbetalingKobling(VEDTAKSPERIODE, UTBETALING_ID)

        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.ferdigstill(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        requireNotNull(totrinnsvurdering)
        assertEquals(VEDTAKSPERIODE, totrinnsvurdering.vedtaksperiodeId)
        assertFalse(totrinnsvurdering.erRetur)
        assertNull(totrinnsvurdering.saksbehandler)
        assertNull(totrinnsvurdering.beslutter)
        assertEquals(1, totrinnsvurdering.utbetalingIdRef)

        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        totrinnsvurderingDao.settSaksbehandler(VEDTAKSPERIODE, SAKSBEHANDLER_OID)
        totrinnsvurderingDao.settBeslutter(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurderingFerdigstilt = totrinnsvurdering()

        requireNotNull(totrinnsvurderingFerdigstilt)
        assertFalse(totrinnsvurderingFerdigstilt.erRetur)
        assertNull(totrinnsvurderingFerdigstilt.saksbehandler)
        assertNull(totrinnsvurderingFerdigstilt.beslutter)
        assertEquals(1, totrinnsvurdering.utbetalingIdRef)
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
        totrinnsvurderingDao.settSaksbehandler(OPPGAVE_ID, SAKSBEHANDLER_OID)

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurderingDao.hentAktiv(VEDTAKSPERIODE)?.saksbehandler)
    }

    @Test
    fun `Håndterer oppdatering av totrinnsvurdering som ikke finnes`() {
        opprettPerson()
        opprettSaksbehandler()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.settSaksbehandler(1L, SAKSBEHANDLER_OID)

        assertNull(totrinnsvurderingDao.hentAktiv(1L))
    }

    private fun totrinnsvurdering() = sessionOf(dataSource).use {
        it.run(queryOf("SELECT * FROM totrinnsvurdering").map { row ->
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
}
