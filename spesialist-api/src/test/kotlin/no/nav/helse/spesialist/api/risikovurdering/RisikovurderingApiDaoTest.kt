package no.nav.helse.spesialist.api.risikovurdering

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class RisikovurderingApiDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner risikovurdering`() {
        risikovurdering()
        val risikovurdering = requireNotNull(risikovurderingApiDao.finnRisikovurderinger(FØDSELSNUMMER)[PERIODE.id])
        assertEquals(1, risikovurdering.funn.size)
        assertEquals(1, risikovurdering.kontrollertOk.size)
        assertEquals("En beskrivelse", risikovurdering.funn.first()["beskrivelse"].asText())
        assertEquals("En annen beskrivelse", risikovurdering.kontrollertOk.first()["beskrivelse"].asText())
        assertNotNull(risikovurdering.kontrollertOk.first()["kategori"])
    }

    @Test
    fun `mapper verdier fra løsning`() {
        @Language("json")
        val data = """
            {
                "funn": [{
                    "kategori": [],
                    "beskrivelse": "Liten bedrift (2 ansatte)"
                }],
                "kontrollertOk": [{
                    "beskrivelse": "Eierandel i selskap (0 prosent)",
                    "kategori": ["REL"]
                }]
            }
        """
        risikovurdering(data = data)
        val risikovurdering = requireNotNull(risikovurderingApiDao.finnRisikovurderinger(FØDSELSNUMMER)[PERIODE.id])
        risikovurdering.also { dto ->
            assertEquals(listOf("Eierandel i selskap (0 prosent)"), dto.kontrollertOk.map { it["beskrivelse"].asText() })
            assertEquals(listOf("REL"), dto.kontrollertOk.flatMap { it["kategori"].map(JsonNode::asText) })

            assertEquals(listOf("Liten bedrift (2 ansatte)"), dto.funn.map { it["beskrivelse"].asText() })
            assertEquals(emptyList<String>(), dto.funn.flatMap { it["kategori"].map(JsonNode::asText) })
        }
    }

    @Test
    fun `leser manglende risikovurdering`() {
        assertNull(risikovurderingApiDao.finnRisikovurderinger(FØDSELSNUMMER)[PERIODE.id])
    }

    private fun risikovurdering(vedtaksperiodeId: UUID = PERIODE.id, data: String = riskrespons) {
        opprettVedtak(
            opprettPerson(),
            opprettArbeidsgiver(),
            periode = Periode(vedtaksperiodeId, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
        )
        dbQuery.update(
            """
            INSERT INTO risikovurdering_2021 (vedtaksperiode_id, kan_godkjennes_automatisk, data)
            VALUES (:vedtaksperiodeId, true, :data::json)
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "data" to data
        )
    }

    @Language("JSON")
    private val riskrespons = """
    {
        "vedtaksperiodeId": "${UUID.randomUUID()}",
        "funn": [
            {
                "beskrivelse": "En beskrivelse"
            }
        ],
        "kontrollertOk": [
            {
                "beskrivelse": "En annen beskrivelse",
                "kategori": [
                    "0-0"
                ]
            }
        ]
    }
    """
}
