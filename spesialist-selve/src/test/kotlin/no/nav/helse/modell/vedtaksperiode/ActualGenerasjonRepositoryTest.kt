package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class GenerasjonRepositoryTest : AbstractDatabaseTest() {

    private val repository = ActualGenerasjonRepository(dataSource)

    @Test
    fun `kan opprette første generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()

        repository.opprettFørste(vedtaksperiodeId, hendelseId)
        
        assertGenerasjon(vedtaksperiodeId, hendelseId)
    }

    @Test
    fun `kan ikke opprette FØRSTE generasjon når det eksisterer generasjoner fra før av`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet1 = UUID.randomUUID()
        val vedtaksperiodeOpprettet2 = UUID.randomUUID()

        repository.opprettFørste(vedtaksperiodeId, vedtaksperiodeOpprettet1)
        repository.opprettFørste(vedtaksperiodeId, vedtaksperiodeOpprettet2)

        assertGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet1)
        assertIngenGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet2)
    }

    @Test
    fun `kan opprette neste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet = UUID.randomUUID()
        val vedtaksperiodeEndret = UUID.randomUUID()
        val vedtakFattet = UUID.randomUUID()

        repository.opprettFørste(vedtaksperiodeId, vedtaksperiodeOpprettet)
        repository.låsFor(vedtaksperiodeId, vedtakFattet)
        repository.forsøkOpprett(vedtaksperiodeId, vedtaksperiodeEndret)

        assertGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet)
        assertGenerasjon(vedtaksperiodeId, vedtaksperiodeEndret)
    }

    @Test
    fun `kan ikke opprette ny generasjon når tidligere er ulåst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet = UUID.randomUUID()
        val vedtaksperiodeEndret = UUID.randomUUID()

        repository.opprettFørste(vedtaksperiodeId, vedtaksperiodeOpprettet)
        repository.forsøkOpprett(vedtaksperiodeId, vedtaksperiodeEndret)

        assertGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet)
        assertIngenGenerasjon(vedtaksperiodeId, vedtaksperiodeEndret)
    }

    @Test
    fun `kan knytte utbetalingId til generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        repository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        repository.utbetalingFor(vedtaksperiodeId, utbetalingId)

        assertUtbetaling(vedtaksperiodeId, utbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som ikke har utbetalingId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        repository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        repository.låsFor(vedtaksperiodeId, UUID.randomUUID())
        repository.utbetalingFor(vedtaksperiodeId, utbetalingId)

        assertUtbetaling(vedtaksperiodeId, null)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som har utbetalingId fra før`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val gammel = UUID.randomUUID()
        val ny = UUID.randomUUID()

        repository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        repository.utbetalingFor(vedtaksperiodeId, gammel)
        repository.låsFor(vedtaksperiodeId, UUID.randomUUID())
        repository.utbetalingFor(vedtaksperiodeId, ny)

        assertUtbetaling(vedtaksperiodeId, gammel)
        assertUtbetaling(vedtaksperiodeId, gammel)
    }

    @Test
    fun `finner siste generasjon for en periode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()

        repository.opprettFørste(vedtaksperiodeId, hendelseId)
        assertGenerasjon(vedtaksperiodeId, hendelseId)
        assertDoesNotThrow {
            repository.sisteFor(vedtaksperiodeId)
        }
    }

    @Test
    fun `kaster exception dersom vi ikke finner generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()

        assertThrows<IllegalStateException> {
            repository.sisteFor(vedtaksperiodeId)
        }
    }

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

    private fun assertUtbetaling(vedtaksperiodeId: UUID, forventetUtbetalingId: UUID?) {
        val utbetalingId = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT utbetaling_id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1;"

            session.run(queryOf(query, vedtaksperiodeId).map {
                it.uuidOrNull("utbetaling_id")
            }.asSingle)
        }

        assertEquals(forventetUtbetalingId, utbetalingId)
    }
}