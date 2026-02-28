package no.nav.helse.spesialist.db.dao.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PgRisikovurderingApiDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver).also { opprettBehandling(it) }

    @Test
    fun `finner risikovurdering`() {
        risikovurdering(riskrespons)
        val risikovurdering = requireNotNull(risikovurderingApiDao.finnRisikovurderinger(person.id.value)[vedtaksperiode.id.value])
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
        risikovurdering(data)
        val risikovurdering = requireNotNull(risikovurderingApiDao.finnRisikovurderinger(person.id.value)[vedtaksperiode.id.value])
        risikovurdering.also { dto ->
            assertEquals(
                listOf("Eierandel i selskap (0 prosent)"),
                dto.kontrollertOk.map { it["beskrivelse"].asText() },
            )
            assertEquals(listOf("REL"), dto.kontrollertOk.flatMap { it["kategori"].map(JsonNode::asText) })

            assertEquals(listOf("Liten bedrift (2 ansatte)"), dto.funn.map { it["beskrivelse"].asText() })
            assertEquals(emptyList<String>(), dto.funn.flatMap { it["kategori"].map(JsonNode::asText) })
        }
    }

    @Test
    fun `leser manglende risikovurdering`() {
        assertNull(risikovurderingApiDao.finnRisikovurderinger(person.id.value)[vedtaksperiode.id.value])
    }

    private fun risikovurdering(data: String) {
        dbQuery.update(
            """
            INSERT INTO risikovurdering_2021 (vedtaksperiode_id, kan_godkjennes_automatisk, data)
            VALUES (:vedtaksperiodeId, true, :data::json)
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiode.id.value,
            "data" to data,
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
