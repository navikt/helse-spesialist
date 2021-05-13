package no.nav.helse.modell.risiko

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RisikovurderingTest {

    @Test
    fun `Vurdering kan behandles automatisk`() {
        val risikovurdering = Risikovurdering.restore(true)
        assertTrue(risikovurdering.erAautomatiserbar())
    }

    @Test
    fun `Vurdering kan ikke behandles automatisk`() {
        val risikovurdering = Risikovurdering.restore(false)
        assertFalse(risikovurdering.erAautomatiserbar())
    }
}
