package no.nav.helse.db

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.juli
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AvslagDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagrer avslag`() {
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
    fun `finner avslag`() {
        opprettPerson()
        opprettSaksbehandler()
        totrinnsvurderingDao.opprett(VEDTAKSPERIODE)
        settSaksbehandler(VEDTAKSPERIODE, SAKSBEHANDLER_OID)

        val totrinnsvurdering = totrinnsvurdering()

        assertEquals(SAKSBEHANDLER_OID, totrinnsvurdering?.saksbehandler)
        assertNotNull(totrinnsvurdering?.oppdatert)
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
