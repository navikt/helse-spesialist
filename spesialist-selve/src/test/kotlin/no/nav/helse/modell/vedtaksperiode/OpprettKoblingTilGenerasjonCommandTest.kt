package no.nav.helse.modell.vedtaksperiode

import ToggleHelpers.enable
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.kommando.CommandContext
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpprettKoblingTilGenerasjonCommandTest: AbstractDatabaseTest() {
    private val repository = ActualGenerasjonRepository(dataSource)
    private val vedtaksperiodeId = UUID.randomUUID()
    private val utbetalingId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()
    private val command = OpprettKoblingTilGenerasjonCommand(hendelseId, vedtaksperiodeId, utbetalingId, repository)
    @Test
    fun `Opprett generasjon dersom det ikke finnes noen generasjon`() {
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, repository.tilhørendeFor(utbetalingId).size)
        assertGenerasjonerFor(vedtaksperiodeId, 1)
        assertDoesNotThrow {
            repository.sisteFor(vedtaksperiodeId)
        }
    }

    @Test
    fun `Oppretter ikke noen ny generasjon dersom det eksisterer en generasjon fra før av og denne er ulåst`() {
        val generasjon = repository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        generasjon?.håndterNyUtbetaling(hendelseId, utbetalingId)
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, repository.tilhørendeFor(utbetalingId).size)
        assertEquals(generasjon, repository.sisteFor(vedtaksperiodeId))
        assertGenerasjonerFor(vedtaksperiodeId, 1)
    }

    @Test
    fun `Oppretter ny generasjon dersom det eksisterer en generasjon fra før av og denne er låst`() {
        val generasjon = repository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        generasjon?.håndterVedtakFattet(UUID.randomUUID())
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, repository.tilhørendeFor(utbetalingId).size)
        assertNotEquals(generasjon, repository.sisteFor(vedtaksperiodeId))
        assertEquals(repository.tilhørendeFor(utbetalingId).last(), repository.sisteFor(vedtaksperiodeId))
        assertGenerasjonerFor(vedtaksperiodeId, 2)
    }

    private fun assertGenerasjonerFor(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg WHERE vedtaksperiode_id = ?"
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    @BeforeEach
    internal fun beforeEach() {
        Toggle.VedtaksperiodeGenerasjoner.enable()
    }

    @AfterEach
    internal fun afterEach() {
        Toggle.VedtaksperiodeGenerasjoner.enable()
    }
}