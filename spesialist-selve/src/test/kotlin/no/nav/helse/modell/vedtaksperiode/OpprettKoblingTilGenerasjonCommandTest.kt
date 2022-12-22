package no.nav.helse.modell.vedtaksperiode

import ToggleHelpers.enable
import java.util.UUID
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpprettKoblingTilGenerasjonCommandTest: AbstractDatabaseTest() {
    private val repository = ActualGenerasjonRepository(dataSource)
    private val vedtaksperiodeId = UUID.randomUUID()
    private val utbetalingId = UUID.randomUUID()
    private val command = OpprettKoblingTilGenerasjonCommand(UUID.randomUUID(), vedtaksperiodeId, utbetalingId, repository)
    @Test
    fun `Opprett generasjon dersom det ikke finnes noen generasjon`() {
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, repository.tilhørendeFor(utbetalingId).size)
        assertDoesNotThrow {
            repository.sisteFor(vedtaksperiodeId)
        }
    }

    @Test
    fun `Oppretter ikke noen ny generasjon dersom det eksisterer en generasjon fra før av`() {
        val generasjon = repository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        generasjon?.håndterNyUtbetaling(utbetalingId)
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, repository.tilhørendeFor(utbetalingId).size)
        assertEquals(generasjon, repository.sisteFor(vedtaksperiodeId))
    }

    @Test
    fun `Oppretter ikke noen ny generasjon dersom det eksisterer en generasjon fra før av og denne er låst`() {
        val generasjon = repository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        generasjon?.håndterVedtakFattet(UUID.randomUUID())
        generasjon?.håndterNyUtbetaling(utbetalingId)
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(0, repository.tilhørendeFor(utbetalingId).size)
        assertEquals(generasjon, repository.sisteFor(vedtaksperiodeId))
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