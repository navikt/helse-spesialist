package no.nav.helse.modell.påvent

import DatabaseIntegrationTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PåVentDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagre påvent`() {
        nyPerson()
        val frist = LocalDate.now().plusDays(21)
        påVentDao.lagrePåVent(OPPGAVE_ID, SAKSBEHANDLER_OID, frist, "begrunnelse X")
        val påVent = påvent()
        assertEquals(1, påVent.size)
        påVent.first().assertEquals(1, VEDTAKSPERIODE, SAKSBEHANDLER_OID, frist, "begrunnelse X")
    }

    @Test
    fun `slett påvent`() {
        nyPerson()
        val frist = LocalDate.now().plusDays(21)
        påVentDao.lagrePåVent(oppgaveId, SAKSBEHANDLER_OID, frist, "begrunnelse X")
        val påVent = påvent()
        assertEquals(1, påVent.size)
        påVentDao.slettPåVent(oppgaveId)
        val påVentEtterSletting = påvent()
        assertEquals(0, påVentEtterSletting.size)
    }

    @Test
    fun `finnes påvent`() {
        nyPerson()
        val frist = LocalDate.now().plusDays(21)
        påVentDao.lagrePåVent(OPPGAVE_ID, SAKSBEHANDLER_OID, frist, "begrunnelse X")
        val erPåVent = påVentDao.erPåVent(VEDTAKSPERIODE)
        assertTrue(erPåVent)
    }

    private fun påvent() = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT * FROM pa_vent").map { row ->
                PåVent(
                    row.int("id"),
                    row.uuid("vedtaksperiode_id"),
                    row.uuid("saksbehandler_ref"),
                    row.localDateOrNull("frist"),
                    row.stringOrNull("begrunnelse"),
                    row.localDateTime("opprettet"),
                )
            }.asList)
    }

    private class PåVent(
        private val id: Int,
        private val vedtaksperiodeId: UUID,
        private val saksbehandlerRef: UUID,
        private val frist: LocalDate?,
        private val begrunnelse: String?,
        private val opprettet: LocalDateTime,
    ) {
        fun assertEquals(
            forventetId: Int,
            forventetVedtaksperiodeId: UUID,
            forventetSaksbehandlerRef: UUID,
            forventetFrist: LocalDate?,
            forventetBegrunnelse: String?,
        ) {
            assertEquals(forventetId, id)
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetSaksbehandlerRef, saksbehandlerRef)
            assertEquals(forventetFrist, frist)
            assertEquals(forventetBegrunnelse, begrunnelse)
            assertNotNull(opprettet)
        }
    }
}
