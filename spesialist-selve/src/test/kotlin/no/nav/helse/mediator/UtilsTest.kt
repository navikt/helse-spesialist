package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class UtilsTest {
    @Test
    fun `asUUID henter UUID fra en JsonNode`() {
        val uuid = UUID.randomUUID()
        val jsonNode = objectMapper.valueToTree<JsonNode>(mapOf("@id" to uuid))
        assertEquals(uuid, jsonNode["@id"].asUUID())
    }

    @Test
    fun `asUUID kaster exception hvis feltet ikke finnes i JsonNoden`() {
        val jsonNode = objectMapper.valueToTree<JsonNode>(emptyMap<String, String>())
        assertThrows<NullPointerException> { jsonNode["@id"].asUUID() }
    }

    @Test
    fun `asUUID kaster exception hvis feltet inneholder noe annet enn en UUID`() {
        val jsonNode = objectMapper.valueToTree<JsonNode>(mapOf("hei" to "hallo"))
        assertThrows<IllegalArgumentException> { jsonNode["hei"].asUUID() }
    }
}
