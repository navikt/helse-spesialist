package no.nav.helse.db.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PgRisikovurderingApiDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `finner risikovurdering`() {
        risikovurdering(riskrespons)
        val risikovurdering = requireNotNull(risikovurderingApiDao.finnRisikovurderinger(FNR)[PERIODE.id])
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
        val risikovurdering = requireNotNull(risikovurderingApiDao.finnRisikovurderinger(FNR)[PERIODE.id])
        risikovurdering.also { dto ->
            assertEquals(
                listOf("Eierandel i selskap (0 prosent)"),
                dto.kontrollertOk.map { it["beskrivelse"].asText() })
            assertEquals(listOf("REL"), dto.kontrollertOk.flatMap { it["kategori"].map(JsonNode::asText) })

            assertEquals(listOf("Liten bedrift (2 ansatte)"), dto.funn.map { it["beskrivelse"].asText() })
            assertEquals(emptyList<String>(), dto.funn.flatMap { it["kategori"].map(JsonNode::asText) })
        }
    }

    @Test
    fun `leser manglende risikovurdering`() {
        assertNull(risikovurderingApiDao.finnRisikovurderinger(FNR)[PERIODE.id])
    }

    private fun risikovurdering(data: String) {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        dbQuery.update(
            """
            INSERT INTO risikovurdering_2021 (vedtaksperiode_id, kan_godkjennes_automatisk, data)
            VALUES (:vedtaksperiodeId, true, :data::json)
            """.trimIndent(),
            "vedtaksperiodeId" to PERIODE.id,
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
