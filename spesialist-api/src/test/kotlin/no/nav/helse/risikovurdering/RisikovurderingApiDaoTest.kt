package no.nav.helse.risikovurdering

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.*
import org.junit.jupiter.api.Assertions.assertNull

internal class RisikovurderingApiDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finner risikovurdering`() {
        risikovurdering()
        val risikovurdering = requireNotNull(risikovurderingApiDao.finnRisikovurdering(PERIODE.first))
        assertEquals(1, risikovurdering.funn.size)
        assertEquals(1, risikovurdering.kontrollertOk.size)
        assertEquals("En beskrivelse", risikovurdering.funn.first()["beskrivelse"].asText())
        assertEquals(listOf("En beskrivelse"), risikovurdering.arbeidsuførhetvurdering)
        assertEquals("En annen beskrivelse", risikovurdering.kontrollertOk.first()["beskrivelse"].asText())
        assertNotNull(risikovurdering.kontrollertOk.first()["kategori"])
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
        """
        risikovurdering(data = data)
        val risikovurdering = requireNotNull(risikovurderingApiDao.finnRisikovurdering(PERIODE.first))
        risikovurdering.also { dto ->
            assertEquals(listOf("jobb ok"), dto.kontrollertOk.map { it["beskrivelse"].asText() })
            assertEquals(listOf("arbeid"), dto.kontrollertOk.flatMap { it["kategori"].map(JsonNode::asText) })

            assertEquals(listOf("8-4 ikke ok"), dto.funn.map { it["beskrivelse"].asText() })
            assertEquals(listOf("8-4"), dto.funn.flatMap { it["kategori"].map(JsonNode::asText) })
            assertEquals(false, dto.funn.first()["kreverSupersaksbehandler"].asBoolean())
        }
    }

    @Test
    fun `leser manglende risikovurdering`() {
        assertNull(risikovurderingApiDao.finnRisikovurdering(UUID.randomUUID()))
    }

    private fun risikovurdering(vedtaksperiodeId: UUID = PERIODE.first, data: String = riskrespons) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO risikovurdering_2021(vedtaksperiode_id, kan_godkjennes_automatisk, krever_supersaksbehandler, data) VALUES(?, True, False, ?::json)"
        session.run(queryOf(statement, vedtaksperiodeId, data).asExecute)
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
