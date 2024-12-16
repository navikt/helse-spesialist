package no.nav.helse.e2e

import AbstractE2ETest
import kotliquery.sessionOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.modell.vedtaksperiode.Behandling
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
