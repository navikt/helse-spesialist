package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.modell.kommando.CommandContext
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpprettKoblingTilGenerasjonCommandTest: AbstractDatabaseTest() {
    private val generasjonRepository = ActualGenerasjonRepository(dataSource)
    private val vedtaksperiodeId = UUID.randomUUID()
    private val utbetalingId = UUID.randomUUID()
    private val hendelseId = UUID.randomUUID()
    private val generasjon = Generasjon.håndterVedtaksperiodeOpprettet(vedtaksperiodeId, 1.januar, 31.januar, 1.januar).also {
        it.registrer(generasjonRepository)
    }
    private val command = OpprettKoblingTilGenerasjonCommand(
        hendelseId = hendelseId,
        utbetalingId = utbetalingId,
        gjeldendeGenerasjon = generasjon
    )
    @Test
    fun `Opprett kobling til utbetaling for generasjon`() {
        generasjon.håndterVedtaksperiodeOpprettet(UUID.randomUUID())
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, generasjonRepository.tilhørendeFor(utbetalingId).size)
        assertGenerasjonerFor(vedtaksperiodeId, 1)
    }

    private fun assertGenerasjonerFor(vedtaksperiodeId: UUID, forventetAntall: Int) {
        @Language("PostgreSQL")
        val query = "SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg WHERE vedtaksperiode_id = ?"
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }
}