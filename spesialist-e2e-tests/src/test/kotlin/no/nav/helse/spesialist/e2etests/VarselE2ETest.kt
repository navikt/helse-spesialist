package no.nav.helse.spesialist.e2etests

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class VarselE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `ingen varsel`() {
        // Given:

        // When:
        simulerPublisertSendtSøknadNavMelding()
        val spleisBehandlingId = UUID.randomUUID()
        simulerPublisertBehandlingOpprettetMelding(spleisBehandlingId = spleisBehandlingId)
        simulerPublisertVedtaksperiodeNyUtbetalingMelding()
        simulerPublisertUtbetalingEndretMelding()
        simulerPublisertVedtaksperiodeEndretMelding()
        simulerPublisertGodkjenningsbehovMelding(spleisBehandlingId = spleisBehandlingId)

        // Then:
        assertEquals(emptySet(), hentVarselkoder())
    }
}
