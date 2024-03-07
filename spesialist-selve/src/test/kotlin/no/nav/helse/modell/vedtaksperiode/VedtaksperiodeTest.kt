package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtaksperiodeTest {

    @Test
    fun `Kan ikke gjenopprette vedtaksperiode uten generasjoner`() {
        assertThrows<IllegalStateException> {
            Vedtaksperiode.gjenopprett(UUID.randomUUID(), emptyList())
        }
    }
}