package no.nav.helse.spesialist.db.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class PgPåVentDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `lagre påvent`() {
        nyPerson()
        val frist = LocalDate.now().plusDays(21)
        val dialogId = dialogDao.lagre()
        påVentDao.lagrePåVent(OPPGAVE_ID, SAKSBEHANDLER_OID, frist, emptyList(), null, dialogId)
        val påVent = påvent()
        assertEquals(1, påVent.size)
        påVent.first().assertEquals(VEDTAKSPERIODE, SAKSBEHANDLER_OID, frist, emptyList(), null)
    }

    @Test
    fun `lagre påvent med årsaker`() {
        nyPerson()
        val frist = LocalDate.now().plusDays(21)
        val dialogId = dialogDao.lagre()
        påVentDao.lagrePåVent(
            OPPGAVE_ID,
            SAKSBEHANDLER_OID,
            frist,
            listOf(PåVentÅrsak("key1", "årsak1"), PåVentÅrsak("key2", "årsak2")),
            "Et notat",
            dialogId,
        )
        val påVent = påvent()
        assertEquals(1, påVent.size)
        påVent.first().assertEquals(VEDTAKSPERIODE, SAKSBEHANDLER_OID, frist, listOf("årsak1", "årsak2"), "Et notat")
    }

    @Test
    fun `lagre påvent med årsaker - oppdatere frist`() {
        nyPerson()
        val frist = LocalDate.now().plusDays(10)
        val dialogId1 = dialogDao.lagre()
        påVentDao.lagrePåVent(
            OPPGAVE_ID,
            SAKSBEHANDLER_OID,
            frist,
            listOf(PåVentÅrsak("key1", "årsak1"), PåVentÅrsak("key2", "årsak2")),
            "Et notat",
            dialogId1,
        )
        val påVent = påvent()
        assertEquals(1, påVent.size)
        påVent.first().assertEquals(VEDTAKSPERIODE, SAKSBEHANDLER_OID, frist, listOf("årsak1", "årsak2"), "Et notat")

        val nyFrist = frist.plusDays(10)
        val dialogId2 = dialogDao.lagre()
        påVentDao.oppdaterPåVent(
            OPPGAVE_ID,
            SAKSBEHANDLER_OID,
            nyFrist,
            listOf(PåVentÅrsak("key1", "årsak1")),
            "Et nytt notat",
            dialogId2,
        )

        val påVentNyFrist = påvent()
        assertEquals(1, påVentNyFrist.size)
        påVentNyFrist.first().assertEquals(VEDTAKSPERIODE, SAKSBEHANDLER_OID, nyFrist, listOf("årsak1"), "Et nytt notat")
    }

    @Test
    fun `slett påvent`() {
        nyPerson()
        val frist = LocalDate.now().plusDays(21)
        val dialogId = dialogDao.lagre()
        påVentDao.lagrePåVent(oppgaveId, SAKSBEHANDLER_OID, frist, emptyList(), null, dialogId)
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
        val dialogId = dialogDao.lagre()
        påVentDao.lagrePåVent(OPPGAVE_ID, SAKSBEHANDLER_OID, frist, emptyList(), null, dialogId)
        val erPåVent = påVentDao.erPåVent(VEDTAKSPERIODE)
        assertTrue(erPåVent)
    }

    private fun påvent(vedtaksperiodeId: UUID = VEDTAKSPERIODE) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "SELECT * FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId"
            session.run(
                queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId))
                    .map { row ->
                        PåVent(
                            row.uuid("vedtaksperiode_id"),
                            row.uuid("saksbehandler_ref"),
                            row.localDateOrNull("frist"),
                            row.localDateTime("opprettet"),
                            row.array<String>("årsaker").toList(),
                            row.stringOrNull("notattekst"),
                        )
                    }.asList,
            )
        }

    private class PåVent(
        private val vedtaksperiodeId: UUID,
        private val saksbehandlerRef: UUID,
        private val frist: LocalDate?,
        private val opprettet: LocalDateTime,
        private val årsaker: List<String>,
        private val notatTekst: String?,
    ) {
        fun assertEquals(
            forventetVedtaksperiodeId: UUID,
            forventetSaksbehandlerRef: UUID,
            forventetFrist: LocalDate?,
            forventetÅrsaker: List<String>,
            forventetNotatTekst: String?,
        ) {
            assertEquals(forventetVedtaksperiodeId, vedtaksperiodeId)
            assertEquals(forventetSaksbehandlerRef, saksbehandlerRef)
            assertEquals(forventetFrist, frist)
            assertNotNull(opprettet)
            assertEquals(forventetÅrsaker, årsaker)
            assertEquals(forventetNotatTekst, notatTekst)
        }
    }
}
