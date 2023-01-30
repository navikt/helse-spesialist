package no.nav.helse.modell.vedtaksperiode

import ToggleHelpers.enable
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.ActualVarselRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpprettKoblingTilGenerasjonCommandTest: AbstractDatabaseTest() {
    private val generasjonRepository = ActualGenerasjonRepository(dataSource)
    private val varselRepository = ActualVarselRepository(dataSource)
    private val vedtaksperiodeId = UUID.randomUUID()
    private val utbetalingId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()
    private val command = OpprettKoblingTilGenerasjonCommand(hendelseId, vedtaksperiodeId, utbetalingId, generasjonRepository, varselRepository)
    @Test
    fun `Opprett generasjon dersom det ikke finnes noen generasjon`() {
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, generasjonRepository.tilhørendeFor(utbetalingId).size)
        assertGenerasjonerFor(vedtaksperiodeId, 1)
        assertDoesNotThrow {
            generasjonRepository.sisteFor(vedtaksperiodeId)
        }
    }

    @Test
    fun `Oppretter ikke noen ny generasjon dersom det eksisterer en generasjon fra før av og denne er ulåst`() {
        val generasjon = generasjonRepository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        generasjon?.håndterNyUtbetaling(hendelseId, utbetalingId, varselRepository)
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, generasjonRepository.tilhørendeFor(utbetalingId).size)
        assertEquals(generasjon, generasjonRepository.sisteFor(vedtaksperiodeId))
        assertGenerasjonerFor(vedtaksperiodeId, 1)
    }

    @Test
    fun `Oppretter ny generasjon dersom det eksisterer en generasjon fra før av og denne er låst`() {
        val generasjon = generasjonRepository.opprettFørste(vedtaksperiodeId, UUID.randomUUID())
        generasjon?.håndterVedtakFattet(UUID.randomUUID())
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, generasjonRepository.tilhørendeFor(utbetalingId).size)
        assertNotEquals(generasjon, generasjonRepository.sisteFor(vedtaksperiodeId))
        assertEquals(generasjonRepository.tilhørendeFor(utbetalingId).last(), generasjonRepository.sisteFor(vedtaksperiodeId))
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