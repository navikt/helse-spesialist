package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `oppretter generasjon for vedtaksperiode`() {
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE_ID, UUID.randomUUID())
        val siste = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)

        assertEquals(generasjon, siste)
    }

    @Test
    fun `kan låse generasjon`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val vedtakFattetId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE_ID, vedtaksperiodeEndretId)
        val låstGenerasjon = generasjonDao.låsFor(VEDTAKSPERIODE_ID, vedtakFattetId)

        assertNotEquals(generasjon, låstGenerasjon)
        assertLåst(VEDTAKSPERIODE_ID, vedtaksperiodeEndretId, vedtakFattetId)
    }

    @Test
    fun `sjekker at siste generasjon blir returnert`() {
        generasjonDao.opprettFor(VEDTAKSPERIODE_ID, UUID.randomUUID())
        generasjonDao.låsFor(VEDTAKSPERIODE_ID, UUID.randomUUID())
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE_ID, UUID.randomUUID())
        val siste = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)

        assertEquals(generasjon, siste)
    }

    private fun assertLåst(vedtaksperiodeId: UUID, opprettetAvId: UUID, låstAvId: UUID) {
        val låst = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT låst FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND opprettet_av_hendelse = ? AND låst_av_hendelse = ?;"

            session.run(queryOf(query, vedtaksperiodeId, opprettetAvId, låstAvId).map {
                it.boolean("låst")
            }.asSingle)
        } ?: false

        assertTrue(låst) {"Generasjonen er ikke låst"}
    }
}