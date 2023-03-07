package no.nav.helse.modell

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.juli
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingDaoTest : DatabaseIntegrationTest() {

    @BeforeAll fun setup() {
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
    fun `Sett er_retur true på totrinnsvurdering`() {
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        totrinnsvurderingDao.settErRetur(VEDTAKSPERIODE)

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
    fun `Oppdaterer utbetalt_utbetaling_ref på totrinnsvurdering`() {
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

    private class Totrinnsvurdering(
        val vedtaksperiodeId: UUID,
        val erRetur: Boolean,
        val saksbehandler: UUID?,
        val beslutter: UUID?,
        val utbetalingIdRef: Long?,
        val opprettet: LocalDateTime,
        val oppdatert: LocalDateTime?,
    )
}
