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
    private val saksbehandler = nyLegacySaksbehandler()
    @Test
    fun `lagre påvent`() {
        val oppgave = nyOppgaveForNyPerson()
        val frist = LocalDate.now().plusDays(21)
        val dialogId = dialogDao.lagre()
        påVentDao.lagrePåVent(oppgave.id, saksbehandler.saksbehandler.id.value, frist, emptyList(), null, dialogId)
        val påVent = påvent(oppgave.vedtaksperiodeId)
        assertEquals(1, påVent.size)
        påVent.first().assertEquals(oppgave.vedtaksperiodeId,
            saksbehandler.saksbehandler.id.value, frist, emptyList(), null)
    }

    @Test
    fun `lagre påvent med årsaker`() {
        val oppgave = nyOppgaveForNyPerson()
        val frist = LocalDate.now().plusDays(21)
        val dialogId = dialogDao.lagre()
        påVentDao.lagrePåVent(
            oppgave.id,
            saksbehandler.saksbehandler.id.value,
            frist,
            listOf(PåVentÅrsak("key1", "årsak1"), PåVentÅrsak("key2", "årsak2")),
            "Et notat",
            dialogId,
        )
        val påVent = påvent(oppgave.vedtaksperiodeId)
        assertEquals(1, påVent.size)
        påVent.first().assertEquals(oppgave.vedtaksperiodeId,
            saksbehandler.saksbehandler.id.value, frist, listOf("årsak1", "årsak2"), "Et notat")
    }

    @Test
    fun `lagre påvent med årsaker - oppdatere frist`() {
        val oppgave = nyOppgaveForNyPerson()
        val frist = LocalDate.now().plusDays(10)
        val dialogId1 = dialogDao.lagre()
        påVentDao.lagrePåVent(
            oppgave.id,
            saksbehandler.saksbehandler.id.value,
            frist,
            listOf(PåVentÅrsak("key1", "årsak1"), PåVentÅrsak("key2", "årsak2")),
            "Et notat",
            dialogId1,
        )
        val påVent = påvent(oppgave.vedtaksperiodeId)
        assertEquals(1, påVent.size)
        påVent.first().assertEquals(oppgave.vedtaksperiodeId,
            saksbehandler.saksbehandler.id.value, frist, listOf("årsak1", "årsak2"), "Et notat")

        val nyFrist = frist.plusDays(10)
        val dialogId2 = dialogDao.lagre()
        påVentDao.oppdaterPåVent(
            oppgave.id,
            saksbehandler.saksbehandler.id.value,
            nyFrist,
            listOf(PåVentÅrsak("key1", "årsak1")),
            "Et nytt notat",
            dialogId2,
        )

        val påVentNyFrist = påvent(oppgave.vedtaksperiodeId)
        assertEquals(1, påVentNyFrist.size)
        påVentNyFrist.first().assertEquals(oppgave.vedtaksperiodeId,
            saksbehandler.saksbehandler.id.value, nyFrist, listOf("årsak1"), "Et nytt notat")
    }

    @Test
    fun `slett påvent`() {
        val oppgave = nyOppgaveForNyPerson()
        val frist = LocalDate.now().plusDays(21)
        val dialogId = dialogDao.lagre()
        påVentDao.lagrePåVent(oppgave.id, saksbehandler.saksbehandler.id.value, frist, emptyList(), null, dialogId)
        val påVent = påvent(oppgave.vedtaksperiodeId)
        assertEquals(1, påVent.size)
        påVentDao.slettPåVent(oppgave.id)
        val påVentEtterSletting = påvent(oppgave.vedtaksperiodeId)
        assertEquals(0, påVentEtterSletting.size)
    }

    @Test
    fun `finnes påvent`() {
        val oppgave = nyOppgaveForNyPerson()

        val frist = LocalDate.now().plusDays(21)
        val dialogId = dialogDao.lagre()
        påVentDao.lagrePåVent(oppgave.id, saksbehandler.saksbehandler.id.value, frist, emptyList(), null, dialogId)
        val erPåVent = påVentDao.erPåVent(oppgave.vedtaksperiodeId)
        assertTrue(erPåVent)
    }

    private fun påvent(vedtaksperiodeId: UUID) =
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
