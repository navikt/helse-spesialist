package no.nav.helse.modell

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.juli
import no.nav.helse.modell.TotrinnsvurderingDao.Totrinnsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingDaoTest : DatabaseIntegrationTest() {

    @BeforeAll
    fun setup() {
        opprettPerson()
    }

    @Test
    fun `opprett totrinnsvurdering`() {
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()
        assertEquals(1, totrinnsvurdering.size)

        assertEquals(VEDTAKSPERIODE, totrinnsvurdering.first().vedtaksperiodeId)
        assertFalse(totrinnsvurdering.first().erRetur)
        assertNull(totrinnsvurdering.first().saksbehandler)
        assertNull(totrinnsvurdering.first().beslutter)
        assertNull(totrinnsvurdering.first().utbetalingIdRef)
        assertNotNull(totrinnsvurdering.first().opprettet)
        assertNull(totrinnsvurdering.first().oppdatert)
    }

    @Test
    fun `Sett saksbehandler på totrinnsvurdering`() {
        opprettSaksbehandler()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settSaksbehandler(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurdering.first().saksbehandler)
        assertNotNull(totrinnsvurdering.first().oppdatert)
    }

    @Test
    fun `Sett beslutter på totrinnsvurdering`() {
        opprettSaksbehandler()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settBeslutter(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurdering.first().beslutter)
        assertNotNull(totrinnsvurdering.first().oppdatert)
    }

    @Test
    fun `Sett beslutter på totrinnsvurdering med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        opprettSaksbehandler()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settBeslutter(1L, SAKSBEHANDLER_OID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurdering.first().beslutter)
        assertNotNull(totrinnsvurdering.first().oppdatert)
    }

    @Test
    fun `Sett er_retur true på totrinnsvurdering`() {
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        assertTrue(totrinnsvurdering.first().erRetur)
        assertNotNull(totrinnsvurdering.first().oppdatert)
    }

    @Test
    fun `Sett er_retur true på totrinnsvurdering med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(1L)

        val totrinnsvurdering = totrinnsvurdering()

        assertTrue(totrinnsvurdering.first().erRetur)
        assertNotNull(totrinnsvurdering.first().oppdatert)
    }

    @Test
    fun `Sett er_retur false på totrinnsvurdering`() {
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        totrinnsvurderingDao.settHåndtertRetur(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        assertFalse(totrinnsvurdering.first().erRetur)
        assertNotNull(totrinnsvurdering.first().oppdatert)
    }

    @Test
    fun `Sett er_retur false på totrinnsvurdering med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        totrinnsvurderingDao.settHåndtertRetur(1L)

        val totrinnsvurdering = totrinnsvurdering()

        assertFalse(totrinnsvurdering.first().erRetur)
        assertNotNull(totrinnsvurdering.first().oppdatert)
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

        assertEquals(1, totrinnsvurdering.first().utbetalingIdRef)
        assertNotNull(totrinnsvurdering.first().oppdatert)
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

        assertEquals(VEDTAKSPERIODE, totrinnsvurdering.first().vedtaksperiodeId)
        assertFalse(totrinnsvurdering.first().erRetur)
        assertNull(totrinnsvurdering.first().saksbehandler)
        assertNull(totrinnsvurdering.first().beslutter)
        assertEquals(1, totrinnsvurdering.first().utbetalingIdRef)

        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        totrinnsvurderingDao.settSaksbehandler(VEDTAKSPERIODE, SAKSBEHANDLER_OID)
        totrinnsvurderingDao.settBeslutter(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurderingFerdigstilt = totrinnsvurdering()

        assertFalse(totrinnsvurderingFerdigstilt.first().erRetur)
        assertNull(totrinnsvurderingFerdigstilt.first().saksbehandler)
        assertNull(totrinnsvurderingFerdigstilt.first().beslutter)
        assertEquals(1, totrinnsvurdering.first().utbetalingIdRef)
    }

    @Test
    fun `Finner aktiv ikke-utbetalt totrinnsvurdering`() {
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)
        val aktivTotrinnsvurdering = totrinnsvurderingDao.hentAktiv(VEDTAKSPERIODE)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(aktivTotrinnsvurdering?.vedtaksperiodeId, totrinnsvurdering.first().vedtaksperiodeId)
        assertEquals(aktivTotrinnsvurdering?.erRetur, totrinnsvurdering.first().erRetur)
        assertEquals(aktivTotrinnsvurdering?.saksbehandler, totrinnsvurdering.first().saksbehandler)
        assertEquals(aktivTotrinnsvurdering?.beslutter, totrinnsvurdering.first().beslutter)
        assertEquals(aktivTotrinnsvurdering?.utbetalingIdRef, totrinnsvurdering.first().utbetalingIdRef)
        assertEquals(aktivTotrinnsvurdering?.oppdatert, totrinnsvurdering.first().oppdatert)
        assertEquals(aktivTotrinnsvurdering?.opprettet, totrinnsvurdering.first().opprettet)
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
        val aktivTotrinnsvurdering = totrinnsvurderingDao.hentAktiv(1L)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(aktivTotrinnsvurdering?.vedtaksperiodeId, totrinnsvurdering.first().vedtaksperiodeId)
        assertEquals(aktivTotrinnsvurdering?.erRetur, totrinnsvurdering.first().erRetur)
        assertEquals(aktivTotrinnsvurdering?.saksbehandler, totrinnsvurdering.first().saksbehandler)
        assertEquals(aktivTotrinnsvurdering?.beslutter, totrinnsvurdering.first().beslutter)
        assertEquals(aktivTotrinnsvurdering?.utbetalingIdRef, totrinnsvurdering.first().utbetalingIdRef)
        assertEquals(aktivTotrinnsvurdering?.oppdatert, totrinnsvurdering.first().oppdatert)
        assertEquals(aktivTotrinnsvurdering?.opprettet, totrinnsvurdering.first().opprettet)
    }

    @Test
    fun `Kan sette saksbehandler med oppgave id`() {
        opprettPerson()
        opprettSaksbehandler()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settSaksbehandler(1L, SAKSBEHANDLER_OID)

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
            Totrinnsvurdering(
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                erRetur = row.boolean("er_retur"),
                saksbehandler = row.uuidOrNull("saksbehandler"),
                beslutter = row.uuidOrNull("beslutter"),
                utbetalingIdRef = row.longOrNull("utbetaling_id_ref"),
                opprettet = row.localDateTime("opprettet"),
                oppdatert = row.localDateTimeOrNull("oppdatert")
            )
        }.asList)
    }
}
