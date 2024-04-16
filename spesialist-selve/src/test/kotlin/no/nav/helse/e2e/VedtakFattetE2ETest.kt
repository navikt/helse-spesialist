package no.nav.helse.e2e

import AbstractE2ETest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID

internal class VedtakFattetE2ETest : AbstractE2ETest() {
    @Test
    fun `vedtak fattet medfører låsing av vedtaksperiode-generasjon`() {
        vedtaksløsningenMottarNySøknad()
        val spleisBehandlingId = UUID.randomUUID()
        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId)
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        assertDoesNotThrow {
            håndterVedtakFattet(spleisBehandlingId = spleisBehandlingId)
        }
        assertFerdigBehandledeGenerasjoner(VEDTAKSPERIODE_ID)
    }

    @Test
    fun `spesialsak er ikke lenger spesialsak når den har vært spesialsak én gang`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        opprettSpesialsak(VEDTAKSPERIODE_ID)
        assertSpesialsak(VEDTAKSPERIODE_ID, true)
        håndterSaksbehandlerløsning()
        håndterVedtakFattet()
        assertSpesialsak(VEDTAKSPERIODE_ID, false)
    }

    private fun opprettSpesialsak(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = """INSERT INTO spesialsak(vedtaksperiode_id) VALUES(?)"""
        sessionOf(Companion.dataSource).use {
            it.run(queryOf(query, vedtaksperiodeId).asExecute)
        }
    }

    private fun assertSpesialsak(
        vedtaksperiodeId: UUID,
        forventetSpesialsak: Boolean,
    ) {
        @Language("PostgreSQL")
        val query = """SELECT true FROM spesialsak WHERE vedtaksperiode_id = ? and ferdigbehandlet = false"""
        val erSpesialsak =
            sessionOf(Companion.dataSource).use {
                it.run(queryOf(query, vedtaksperiodeId).map { it.boolean(1) }.asSingle) ?: false
            }
        assertEquals(forventetSpesialsak, erSpesialsak)
    }

    private fun assertFerdigBehandledeGenerasjoner(vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query = "SELECT 1 FROM selve_vedtaksperiode_generasjon svg WHERE vedtaksperiode_id = :vedtaksperiode_id AND tilstand = '${Generasjon.VedtakFattet.navn()}'"
        val antallFerdigBehandledeGenerasjoner =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId)).map { it.int(1) }.asSingle) ?: 0
            }
        assertTrue(antallFerdigBehandledeGenerasjoner > 0)
    }
}
