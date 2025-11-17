package no.nav.helse.spesialist.application.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import kotlin.test.assertEquals

private val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
private val objectMapperWriter = objectMapper.writerWithDefaultPrettyPrinter()

fun assertJsonEquals(@Language("JSON") expectedJson: String, @Language("JSON") actualJson: String, vararg ignorerStier: String) {
    assertJsonEquals(expectedJson, objectMapper.readTree(actualJson), *ignorerStier)
}

fun assertJsonEquals(@Language("JSON") expectedJson: String, actualJsonNode: JsonNode, vararg ignorerStier: String) {
    val actualAsObjectNode = (actualJsonNode.deepCopy<JsonNode>() as ObjectNode).apply {
        ignorerStier.forEach { sti ->
            val gjeldende: JsonNode = this
            val segmenter = sti.split(".")

            fun følgStiOgFjernLeaf(gjeldende: JsonNode, index: Int) {
                if (index >= segmenter.size) return
                val key = segmenter[index]

                when {
                    gjeldende.isArray -> {
                        for (elem in gjeldende) {
                            følgStiOgFjernLeaf(elem, index)
                        }
                    }

                    gjeldende is ObjectNode -> {
                        if (index == segmenter.lastIndex) {
                            gjeldende.remove(key)
                        } else {
                            val next = gjeldende.get(key) ?: return
                            følgStiOgFjernLeaf(next, index + 1)
                        }
                    }
                }
            }
            følgStiOgFjernLeaf(gjeldende, 0)
        }
    }
    assertEquals(
        expected = objectMapperWriter.writeValueAsString(objectMapper.readTree(expectedJson)),
        actual = objectMapperWriter.writeValueAsString(actualAsObjectNode)
    )
}
