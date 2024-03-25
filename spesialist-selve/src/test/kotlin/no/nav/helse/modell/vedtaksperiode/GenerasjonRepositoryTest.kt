package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.modell.vedtaksperiode.Periode.Companion.til
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GenerasjonRepositoryTest : AbstractDatabaseTest() {

    private val repository = GenerasjonRepository(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)

    @Test
    fun `Exception om vedtaksperioden ikke finnes`() {
        assertThrows<IllegalStateException> {
            repository.brukVedtaksperiode("1234567891011", UUID.randomUUID()) {}
        }
    }

    @Test
    fun `kan knytte utbetalingId til generasjon`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        val generasjon = generasjonDao.opprettFor(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, 1.januar til 31.januar, Generasjon.Ulåst)
        generasjon.registrer(repository)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        assertUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet generasjon som ikke har utbetalingId`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        val generasjon = generasjonDao.opprettFor(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, 1.januar til 31.januar, Generasjon.Ulåst)
        generasjon.registrer(repository)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        assertUtbetaling(generasjonId, null)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet generasjon som har utbetalingId fra før`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val gammel = UUID.randomUUID()
        val ny = UUID.randomUUID()

        val generasjon = generasjonDao.opprettFor(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, 1.januar til 31.januar, Generasjon.Ulåst)
        generasjon.registrer(repository)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammel)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), ny)

        assertUtbetaling(generasjonId, gammel)
    }

    @Test
    fun `Fjern utbetalingId når utbetaling blir forkastet`() {
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, 1.januar til 31.januar, Generasjon.Ulåst)

        generasjon.registrer(repository)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        assertEquals(1, repository.finnVedtaksperiodeIderFor(utbetalingId).size)

        generasjon.håndterForkastetUtbetaling(utbetalingId)
        assertEquals(generasjon(generasjonId, vedtaksperiodeId), generasjon)
        assertEquals(0, repository.finnVedtaksperiodeIderFor(utbetalingId).size)
    }


    private fun generasjon(generasjonId: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )

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