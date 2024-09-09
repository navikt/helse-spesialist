package no.nav.helse.mediator

import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtilsTest {
    @Test
    fun `Er poison pill`() {
        val poisonPills = mapOf("key1" to "redpill", "key2" to "bluepill")
        val jsonNode = objectMapper.createObjectNode()
        jsonNode.put("key2", "bluepill")
        assertTrue(poisonPills.erPoisonPill(jsonNode))
    }

    @Test
    fun `Er ikke poison pill hvis noden ikke inneholder riktig feltnavn`() {
        val poisonPills = mapOf("key1" to "redpill")
        val jsonNode = objectMapper.createObjectNode()
        jsonNode.put("key2", "bluepill")
        assertFalse(poisonPills.erPoisonPill(jsonNode))
    }

    @Test
    fun `Er ikke poison pill`() {
        val poisonPills = mapOf("key" to "redpill")
        val jsonNode = objectMapper.createObjectNode()
        jsonNode.put("key", "bluepill")
        assertFalse(poisonPills.erPoisonPill(jsonNode))
    }

    @Test
    fun `Er ikke poison pill når det ikke finnes poison pills`() {
        val poisonPills = emptyMap<String, String>()
        val jsonNode = objectMapper.createObjectNode()
        jsonNode.put("key", "bluepill")
        assertFalse(poisonPills.erPoisonPill(jsonNode))
    }
}
