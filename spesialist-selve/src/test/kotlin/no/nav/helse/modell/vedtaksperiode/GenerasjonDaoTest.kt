package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {
    private val varselDao = VarselDao(dataSource)

    @Test
    fun `oppretter generasjon for vedtaksperiode`() {
        val generasjon = generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID())
        val siste = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)

        assertEquals(generasjon, siste)
    }

    @Test
    fun `kan låse generasjon`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val vedtakFattetId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, vedtaksperiodeEndretId)
        val låstGenerasjon = generasjonDao.låsFor(generasjonId, vedtakFattetId)

        assertNotEquals(generasjon, låstGenerasjon)
        assertLåst(VEDTAKSPERIODE_ID, vedtaksperiodeEndretId, vedtakFattetId)
    }

    @Test
    fun `gir null tilbake dersom det ikke finnes noen generasjon å låse`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val vedtakFattetId = UUID.randomUUID()
        val låstGenerasjon = generasjonDao.låsFor(VEDTAKSPERIODE_ID, vedtakFattetId)
        assertNull(låstGenerasjon)
        assertIkkeLåst(VEDTAKSPERIODE_ID, vedtaksperiodeEndretId, vedtakFattetId)
    }

    @Test
    fun `gir null tilbake dersom vi ikke finner noen generasjon`() {
        val låstGenerasjon = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)
        assertNull(låstGenerasjon)
    }

    @Test
    fun `sjekker at siste generasjon blir returnert`() {
        val første = generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID())
        generasjonDao.låsFor(VEDTAKSPERIODE_ID, UUID.randomUUID())
        val siste = generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID())
        val funnet = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)

        assertNotEquals(første, funnet)
        assertEquals(siste, funnet)
    }

    @Test
    fun `kan sette utbetaling_id for siste generasjon hvis den er åpen`() {
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, UUID.randomUUID())
        generasjonDao.utbetalingFor(generasjonId, UTBETALING_ID)
        assertUtbetaling(generasjonId, UTBETALING_ID)
    }

    @Test
    fun `generasjon hentes opp sammen med varsler`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID())
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val generasjonId = generasjonIdFor(VEDTAKSPERIODE_ID)
        varselDao.lagreVarsel(varselId, "EN_KODE", varselOpprettet, VEDTAKSPERIODE_ID, generasjonId)
        val generasjon = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)
        assertEquals(
            Generasjon(
                generasjonId,
                VEDTAKSPERIODE_ID,
                UUID.randomUUID(),
                false,
                setOf(
                    Varsel(varselId, "EN_KODE", varselOpprettet, VEDTAKSPERIODE_ID)
                ),
                dataSource
            ),
            generasjon
        )
    }

    @Test
    fun `finner alle generasjoner knyttet til en utbetalingId`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId1, UUID.randomUUID(), UUID.randomUUID())
        generasjonDao.opprettFor(generasjonId2, UUID.randomUUID(), UUID.randomUUID())
        generasjonDao.utbetalingFor(generasjonId1, utbetalingId)
        generasjonDao.utbetalingFor(generasjonId2, utbetalingId)

        assertEquals(2, generasjonDao.alleFor(utbetalingId).size)
    }

    private fun generasjonIdFor(vedtaksperiodeId: UUID): UUID {
        @Language("PostgreSQL")
        val query =
            "SELECT unik_id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY id"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.uuid("unik_id") }.asList).single()
        }
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

    private fun assertIkkeLåst(vedtaksperiodeId: UUID, opprettetAvId: UUID, låstAvId: UUID) {
        val låst = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT låst FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND opprettet_av_hendelse = ? AND låst_av_hendelse = ?;"

            session.run(queryOf(query, vedtaksperiodeId, opprettetAvId, låstAvId).map {
                it.boolean("låst")
            }.asSingle)
        } ?: false

        assertFalse(låst) {"Generasjonen er låst"}
    }

    private fun assertUtbetaling(generasjonId: UUID, forventetUtbetalingId: UUID?) {
        val utbetalingId = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT utbetaling_id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?"

            session.run(queryOf(query, generasjonId).map {
                it.uuidOrNull("utbetaling_id")
            }.asSingle)
        }

        assertEquals(forventetUtbetalingId, utbetalingId)
    }
}