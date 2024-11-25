package no.nav.helse.e2e

import AbstractE2ETest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.modell.vedtaksperiode.Behandling
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
        assertFerdigbehandletGenerasjon(VEDTAKSPERIODE_ID)
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

    private fun assertFerdigbehandletGenerasjon(vedtaksperiodeId: UUID) {
        val query = asSQL(
            """
            SELECT 1 FROM behandling
            WHERE vedtaksperiode_id = :vedtaksperiode_id AND tilstand = :tilstand::generasjon_tilstand
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "tilstand" to Behandling.VedtakFattet.navn(),
        )
        val behandlingenErFerdig =
            sessionOf(dataSource, strict = true).use { it.run(query.map { true }.asSingle) ?: false }
        assertTrue(behandlingenErFerdig)
    }
}
