package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrukerrollerE2ETest : AbstractE2EIntegrationTest() {
    @Test
    fun `returnerer brukerens roller`() {
        // Given: En saksbehandler med spesifikke roller
        saksbehandlerHarTilgang(Brukerrolle.BESLUTTER)
        saksbehandlerHarTilgang(Brukerrolle.KODE_7)

        // When: Kaller GET /api/brukerroller
        val response = callHttpGet("api/brukerroller")

        // Then: Responsen inneholder de forventede rollene
        val roller = response.map { it.asText() }.toSet()
        assertEquals(setOf("BESLUTTER", "KODE_7"), roller)
    }

    @Test
    fun `returnerer tom liste når bruker ikke har roller`() {
        // When: Kaller GET /api/brukerroller uten roller
        val response = callHttpGet(
            relativeUrl = "api/brukerroller",
            brukerroller = emptySet(),
        )

        // Then: Responsen er en tom liste
        assertTrue(response.isEmpty)
    }
}
