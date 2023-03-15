package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import java.time.LocalDate
import java.util.UUID
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.Vedtaksperiode.Companion.oppdaterSykefraværstilfeller
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VedtaksperiodeTest : DatabaseIntegrationTest() {
    private val generasjonRepository = ActualGenerasjonRepository(dataSource)

    @Test
    fun `oppdatere sykefraværstilfelle`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val skjæringstidspunkt1 = 1.januar
        val skjæringstidspunkt2 = 1.februar
        val periode1 = Periode(1.januar, 5.januar)
        val periode2 = Periode(1.februar, 5.februar)
        val vedtaksperiode1 = Vedtaksperiode(vedtaksperiodeId1, skjæringstidspunkt1, periode1)
        val vedtaksperiode2 = Vedtaksperiode(vedtaksperiodeId2, skjæringstidspunkt2, periode2)
        opprettGenerasjon(vedtaksperiodeId1, generasjonId1)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId2)

        listOf(vedtaksperiode1, vedtaksperiode2).oppdaterSykefraværstilfeller(generasjonRepository)

        val generasjon1 = generasjonDao.finnSisteFor(vedtaksperiodeId1)
        val generasjon2 = generasjonDao.finnSisteFor(vedtaksperiodeId2)
        val forventetGenerasjon1 = Generasjon(generasjonId1, vedtaksperiodeId1, null, false, skjæringstidspunkt1, periode1, emptySet(), dataSource)
        val forventetGenerasjon2 = Generasjon(generasjonId2, vedtaksperiodeId2, null, false, skjæringstidspunkt2, periode2, emptySet(), dataSource)

        assertEquals(generasjon1, forventetGenerasjon1)
        assertEquals(generasjon2, forventetGenerasjon2)
    }

    @Test
    fun `oppdatere bare sykefraværstilfelle for åpne generasjoner`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiodeId3 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonId3 = UUID.randomUUID()
        val skjæringstidspunkt1 = 1.januar
        val skjæringstidspunkt2 = 1.februar
        val periode1 = Periode(1.januar, 5.januar)
        val periode2 = Periode(1.februar, 5.februar)
        val vedtaksperiode1 = Vedtaksperiode(vedtaksperiodeId1, skjæringstidspunkt1, periode1)
        val vedtaksperiode2 = Vedtaksperiode(vedtaksperiodeId2, skjæringstidspunkt2, periode2)
        val vedtaksperiode3 = Vedtaksperiode(UUID.randomUUID(), LocalDate.now(), Periode(LocalDate.now(), LocalDate.now()))
        opprettGenerasjon(vedtaksperiodeId1, generasjonId1)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId2)
        opprettGenerasjon(vedtaksperiodeId3, generasjonId3)
        generasjonDao.låsFor(generasjonId3, UUID.randomUUID())

        listOf(vedtaksperiode1, vedtaksperiode2, vedtaksperiode3).oppdaterSykefraværstilfeller(generasjonRepository)

        val generasjon1 = generasjonDao.finnSisteFor(vedtaksperiodeId1)
        val generasjon2 = generasjonDao.finnSisteFor(vedtaksperiodeId2)
        val generasjon3 = generasjonDao.finnSisteFor(vedtaksperiodeId3)
        val forventetGenerasjon1 = Generasjon(generasjonId1, vedtaksperiodeId1, null, false, skjæringstidspunkt1, periode1, emptySet(), dataSource)
        val forventetGenerasjon2 = Generasjon(generasjonId2, vedtaksperiodeId2, null, false, skjæringstidspunkt2, periode2, emptySet(), dataSource)
        val forventetGenerasjon3 = Generasjon(generasjonId3, vedtaksperiodeId3, null, true, null, null, emptySet(), dataSource)

        assertEquals(generasjon1, forventetGenerasjon1)
        assertEquals(generasjon2, forventetGenerasjon2)
        assertEquals(generasjon3, forventetGenerasjon3)
    }

    @Test
    fun `test for flere generasjoner av samme vedtaksperiode - skal bare oppdatere den åpne generasjonen`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonId3 = UUID.randomUUID()
        val skjæringstidspunkt = 1.januar
        val periode = Periode(1.januar, 5.januar)
        val vedtaksperiode = Vedtaksperiode(vedtaksperiodeId1, skjæringstidspunkt, periode)
        opprettGenerasjon(vedtaksperiodeId1, generasjonId1)
        generasjonDao.låsFor(generasjonId1, UUID.randomUUID())
        opprettGenerasjon(vedtaksperiodeId1, generasjonId2)
        generasjonDao.låsFor(generasjonId2, UUID.randomUUID())
        opprettGenerasjon(vedtaksperiodeId1, generasjonId3)

        listOf(vedtaksperiode).oppdaterSykefraværstilfeller(generasjonRepository)

        val generasjon1 = finnGenerasjonMed(generasjonId1)
        val generasjon2 = finnGenerasjonMed(generasjonId2)
        val generasjon3 = finnGenerasjonMed(generasjonId3)
        val forventetGenerasjon1 = Generasjon(generasjonId1, vedtaksperiodeId1, null, true, null, null, emptySet(), dataSource)
        val forventetGenerasjon2 = Generasjon(generasjonId2, vedtaksperiodeId1, null, true, null, null, emptySet(), dataSource)
        val forventetGenerasjon3 = Generasjon(generasjonId3, vedtaksperiodeId1, null, false, skjæringstidspunkt, periode, emptySet(), dataSource)

        assertEquals(generasjon1, forventetGenerasjon1)
        assertEquals(generasjon2, forventetGenerasjon2)
        assertEquals(generasjon3, forventetGenerasjon3)
    }

    private fun finnGenerasjonMed(generasjonId: UUID): Generasjon {
        @Language("PostgreSQL")
        val query = """
            SELECT id, unik_id, vedtaksperiode_id, utbetaling_id, låst, skjæringstidspunkt, fom, tom 
            FROM selve_vedtaksperiode_generasjon 
            WHERE unik_id = :generasjon_id
        """

        return requireNotNull(sessionOf(dataSource).use {session ->
            session.run(queryOf(query, mapOf("generasjon_id" to generasjonId)).map(::toGenerasjon).asSingle)
        })
    }

    private fun toGenerasjon(row: Row): Generasjon {
        return Generasjon(
            row.uuid("unik_id"),
            row.uuid("vedtaksperiode_id"),
            row.uuidOrNull("utbetaling_id"),
            row.boolean("låst"),
            row.localDateOrNull("skjæringstidspunkt"),
            row.localDateOrNull("fom")?.let{
                Periode(
                    it,
                    row.localDate("tom"),
                )
            },
            emptySet(),
            dataSource,
        )
    }
}