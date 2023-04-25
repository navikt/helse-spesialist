package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class GenerasjonRepositoryTest : AbstractDatabaseTest() {

    private val repository = ActualGenerasjonRepository(dataSource)

    @Test
    fun `kan opprette første generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()

        repository.førsteGenerasjonOpprettet(
            UUID.randomUUID(),
            vedtaksperiodeId,
            hendelseId,
            1.januar,
            31.januar,
            1.januar,
            Generasjon.Ulåst
        )
        
        assertGenerasjon(vedtaksperiodeId, hendelseId)
    }

    @Test
    fun `kan ikke opprette FØRSTE generasjon når det eksisterer generasjoner fra før av`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet1 = UUID.randomUUID()
        val vedtaksperiodeOpprettet2 = UUID.randomUUID()

        repository.førsteGenerasjonOpprettet(
            UUID.randomUUID(),
            vedtaksperiodeId,
            vedtaksperiodeOpprettet1,
            1.januar,
            31.januar,
            1.januar,
            Generasjon.Ulåst
        )
        repository.førsteGenerasjonOpprettet(
            UUID.randomUUID(),
            vedtaksperiodeId,
            vedtaksperiodeOpprettet2,
            1.januar,
            31.januar,
            1.januar,
            Generasjon.Ulåst
        )

        assertGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet1)
        assertIngenGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet2)
    }

    @Test
    fun `kan opprette neste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet = UUID.randomUUID()
        val vedtaksperiodeEndret = UUID.randomUUID()
        val vedtakFattet = UUID.randomUUID()
        val førsteGenerasjonId = UUID.randomUUID()
        val andreGenerasjonId = UUID.randomUUID()

        val generasjon = Generasjon(førsteGenerasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        generasjon.registrer(repository)
        generasjon.opprettFørste(vedtaksperiodeOpprettet)
        generasjon.håndterVedtakFattet(vedtakFattet)
        generasjon.håndterVedtaksperiodeEndret(vedtaksperiodeEndret, andreGenerasjonId)

        assertLåstGenerasjon(førsteGenerasjonId, vedtakFattet)
        assertUbehandletGenerasjon(andreGenerasjonId)
    }

    @Test
    fun `kan ikke opprette ny generasjon når tidligere er ulåst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet = UUID.randomUUID()
        val vedtaksperiodeEndret = UUID.randomUUID()
        val generasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        generasjon.registrer(repository)
        generasjon.opprettFørste(vedtaksperiodeOpprettet)
        generasjon.håndterVedtaksperiodeEndret(vedtaksperiodeEndret)

        assertGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet)
        assertIngenGenerasjon(vedtaksperiodeId, vedtaksperiodeEndret)
    }

    @Test
    fun `kan knytte utbetalingId til generasjon`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        generasjon.registrer(repository)
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        assertUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som ikke har utbetalingId`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        generasjon.registrer(repository)
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        assertUtbetaling(generasjonId, null)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som har utbetalingId fra før`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val gammel = UUID.randomUUID()
        val ny = UUID.randomUUID()

        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        generasjon.registrer(repository)
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammel)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), ny)

        assertUtbetaling(generasjonId, gammel)
    }

    @Test
    fun `finner alle generasjoner knyttet til en utbetalingId`() {
        val generasjonIdV1 = UUID.randomUUID()
        val generasjonIdV2 = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        val generasjonV1 = Generasjon(generasjonIdV1, UUID.randomUUID(), 1.januar, 1.januar, 31.januar)
        val generasjonV2 = Generasjon(generasjonIdV2, UUID.randomUUID(), 1.januar, 1.januar, 31.januar)
        generasjonV1.registrer(repository)
        generasjonV2.registrer(repository)
        generasjonV1.opprettFørste(UUID.randomUUID())
        generasjonV2.opprettFørste(UUID.randomUUID())
        generasjonV1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjonV2.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        assertEquals(2, repository.tilhørendeFor(utbetalingId).size)
    }

    @Test
    fun `Fjern utbetalingId når utbetaling blir forkastet`() {
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        generasjon.registrer(repository)
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        assertEquals(1, repository.tilhørendeFor(utbetalingId).size)

        generasjon.invaliderUtbetaling(utbetalingId)
        assertEquals(generasjon(generasjonId, vedtaksperiodeId), generasjon)
        assertEquals(0, repository.tilhørendeFor(utbetalingId).size)
    }


    private fun generasjon(generasjonId: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )

    private fun assertGenerasjon(vedtaksperiodeId: UUID, hendelseId: UUID) {
        val generasjon = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND opprettet_av_hendelse = ?;"

            session.run(queryOf(query, vedtaksperiodeId, hendelseId).map {
                it.long(1)
            }.asSingle)
        }
        assertNotNull(generasjon)
    }

    private fun assertLåstGenerasjon(generasjonId: UUID, hendelseId: UUID) {
        val generasjon = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? AND tilstand_endret_av_hendelse = ?;"

            session.run(queryOf(query, generasjonId, hendelseId).map {
                it.long(1)
            }.asSingle)
        }
        assertNotNull(generasjon)
    }

    private fun assertUbehandletGenerasjon(generasjonId: UUID) {
        val generasjon = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? AND tilstand = '${Generasjon.Ulåst.navn()}';"

            session.run(queryOf(query, generasjonId).map {
                it.long(1)
            }.asSingle)
        }
        assertNotNull(generasjon)
    }

    private fun assertIngenGenerasjon(vedtaksperiodeId: UUID, hendelseId: UUID) {
        val generasjon = sessionOf(dataSource).use {session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND opprettet_av_hendelse = ?;"

            session.run(queryOf(query, vedtaksperiodeId, hendelseId).map {
                it.long(1)
            }.asSingle)
        }
        assertNull(generasjon)
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