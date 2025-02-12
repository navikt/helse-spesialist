package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.kafka.objectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PoisonPillsTest {

    @Test
    fun `Er poison pill`() {
        val poisonPills = PoisonPills(mapOf("key1" to setOf("redpill"), "key2" to setOf("bluepill")))
        assertTrue(poisonPills.erPoisonPill(lagJsonNode("key2", "bluepill")))
    }

    @Test
    fun `Er ikke poison pill hvis noden ikke inneholder riktig feltnavn`() {
        val poisonPills = PoisonPills(mapOf("key1" to setOf("redpill")))
        assertFalse(poisonPills.erPoisonPill(lagJsonNode("key2", "redpill")))
    }

    @Test
    fun `Er ikke poison pill`() {
        val poisonPills = PoisonPills(mapOf("key" to setOf("redpill")))
        assertFalse(poisonPills.erPoisonPill(lagJsonNode("key", "bluepill")))
    }

    @Test
    fun `Er ikke poison pill når det ikke finnes poison pills`() {
        val poisonPills = PoisonPills(emptyMap())
        assertFalse(poisonPills.erPoisonPill(lagJsonNode("key", "bluepill")))
    }

    private fun lagJsonNode(key: String, identifikator: String): JsonNode =
        objectMapper.valueToTree(mapOf(key to identifikator))
}
