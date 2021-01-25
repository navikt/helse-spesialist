package no.nav.helse.modell.risiko

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class RisikovurderingTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
    }

    private val risikovurderingDaoMock = mockk<RisikovurderingDao>()

    @Test
    fun `Vurdering kan behandles automatisk`() {
        every { risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId) }.returns(risikovurderingDto(true))
        val risikovurdering = requireNotNull(
            risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId)?.let { Risikovurdering.restore(it) })
        assertTrue(risikovurdering.erAautomatiserbar())
    }

    @Test
    fun `Vurdering kan ikke behandles automatisk`() {
        every { risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId) }.returns(risikovurderingDto(false))
        val risikovurdering = requireNotNull(
            risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId)?.let { Risikovurdering.restore(it) })
        assertFalse(risikovurdering.erAautomatiserbar())
    }

    @Test
    fun `mapper verdier fra løsning`() {

        @Language("json")
        val data = """
            {
                "funn": [{
                    "kategori": ["8-4"],
                    "beskrivelse": "8-4 ikke ok",
                    "kreverSupersaksbehandler": false
                }],
                "kontrollertOk": [{
                    "kategori": ["arbeid"],
                    "beskrivelse": "jobb ok"
                }]
            }
        """.trimIndent()
        every { risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId) }.returns(
            risikovurderingDto(
                false,
                objectMapper.readTree(data)
            )
        )
        val risikovurdering = requireNotNull(
            risikovurderingDaoMock.hentRisikovurderingDto(vedtaksperiodeId)?.let { Risikovurdering.restore(it) })
        risikovurdering.speilDto().also { dto ->
            assertEquals(listOf("jobb ok"), dto.kontrollertOk.map { it["beskrivelse"].asText() })
            assertEquals(listOf("arbeid"), dto.kontrollertOk.flatMap { it["kategori"].map( JsonNode::asText) })

            assertEquals(listOf("8-4 ikke ok"), dto.funn.map { it["beskrivelse"].asText() })
            assertEquals(listOf("8-4"), dto.funn.flatMap { it["kategori"].map( JsonNode::asText) })
            assertEquals(false, dto.funn.first()["kreverSupersaksbehandler"].asBoolean())
        }
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
