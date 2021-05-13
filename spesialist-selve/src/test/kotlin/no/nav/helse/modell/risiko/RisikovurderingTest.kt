package no.nav.helse.modell.risiko

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class RisikovurderingTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
    }

    @Test
    fun `Vurdering kan behandles automatisk`() {
        val risikovurdering = risikovurderingDto(true).let { Risikovurdering.restore(it) }
        assertTrue(risikovurdering.erAautomatiserbar())
    }

    @Test
    fun `Vurdering kan ikke behandles automatisk`() {
        val risikovurdering = risikovurderingDto(false).let { Risikovurdering.restore(it) }
        assertFalse(risikovurdering.erAautomatiserbar())
    }

    private fun risikovurderingDto(
        kanGodkjennesAutomatisk: Boolean,
        data: JsonNode = objectMapper.createObjectNode().set("funn", objectMapper.createArrayNode())
    ) = RisikovurderingDto(
        vedtaksperiodeId = vedtaksperiodeId,
        opprettet = LocalDateTime.now(),
        kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
        kreverSupersaksbehandler = false,
        data = data,
    )
}
