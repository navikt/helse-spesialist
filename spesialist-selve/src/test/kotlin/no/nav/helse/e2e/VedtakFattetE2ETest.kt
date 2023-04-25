package no.nav.helse.e2e

import AbstractE2ETestV2
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class VedtakFattetE2ETest: AbstractE2ETestV2() {

    @Test
    fun `vedtak fattet medfører låsing av vedtaksperiode-generasjon`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        assertDoesNotThrow {
            håndterVedtakFattet()
        }
        assertFerdigBehandledeGenerasjoner(VEDTAKSPERIODE_ID)
    }

    private fun assertFerdigBehandledeGenerasjoner(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = "SELECT 1 FROM selve_vedtaksperiode_generasjon svg WHERE vedtaksperiode_id = :vedtaksperiode_id AND tilstand = '${Generasjon.Låst.navn()}'"
        val antallFerdigBehandledeGenerasjoner = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).map { it.int(1) }.asSingle) ?: 0
        }
        assertTrue(antallFerdigBehandledeGenerasjoner > 0)
    }
}
