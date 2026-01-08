package no.nav.helse.spesialist.application.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
private val objectMapperWriter = objectMapper.writerWithDefaultPrettyPrinter()

fun assertJsonEquals(
    @Language("JSON") expectedJson: String,
    @Language("JSON") actualJson: String,
    vararg ignorerStier: String,
) {
    assertJsonEquals(expectedJson, objectMapper.readTree(actualJson), *ignorerStier)
}

fun assertJsonEquals(
    @Language("JSON") expectedJson: String,
    actualJsonNode: JsonNode,
    vararg ignorerStier: String,
) {
    val actualAsObjectNode =
        actualJsonNode.deepCopy<JsonNode>().apply {
            ignorerStier.forEach { sti ->
                val gjeldende: JsonNode = this
                val segmenter = sti.split(".")

                fun følgStiOgFjernLeaf(
                    gjeldende: JsonNode,
                    index: Int,
                ) {
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
        actual = objectMapperWriter.writeValueAsString(actualAsObjectNode),
    )
}

fun assertIFortiden(actual: LocalDateTime) {
    val now = LocalDateTime.now()
    assertTrue(
        actual = actual.isBefore(now),
        message = "Forventet at tidspunktet var i fortiden (i forhold til nå: $now), men det var $actual",
    )
}

fun assertMindreEnnNSekunderSiden(
    sekunder: Int,
    actual: LocalDateTime,
) {
    val now = LocalDateTime.now()
    assertTrue(
        actual = actual.isAfter(now.minusSeconds(sekunder.toLong())),
        message = "Forventet at tidspunktet var innenfor $sekunder sekunder tilbake i tid (i forhold til nå: $now), men det var $actual",
    )
}

fun assertEqualsByMicrosecond(
    expected: LocalDateTime?,
    actual: LocalDateTime?,
) {
    assertEquals(expected?.roundToMicros(), actual?.roundToMicros())
}

fun assertEqualsByMicrosecond(
    expected: Instant?,
    actual: Instant?,
) {
    assertEquals(expected?.roundToMicros(), actual?.roundToMicros())
}

fun assertNotEqualsByMicrosecond(
    expected: LocalDateTime?,
    actual: LocalDateTime?,
) {
    assertNotEquals(expected?.roundToMicros(), actual?.roundToMicros())
}

private fun LocalDateTime.roundToMicros(): LocalDateTime {
    val roundUp = (this.nano % 1000) >= 500
    return truncatedTo(ChronoUnit.MICROS).plus(if (roundUp) 1 else 0, ChronoUnit.MICROS)
}

private fun Instant.roundToMicros(): Instant {
    val roundUp = (this.nano % 1000) >= 500
    return truncatedTo(ChronoUnit.MICROS).plus(if (roundUp) 1 else 0, ChronoUnit.MICROS)
}

fun assertIsNumber(actual: JsonNode?) {
    assertEquals(true, actual?.takeUnless { it.isNull }?.isNumber)
}

fun assertAfter(
    expectedAfter: Instant,
    actual: Instant,
) {
    assertTrue(actual.isAfter(expectedAfter), "Forventet tidspunkt etter $expectedAfter, men var $actual")
}

fun assertAfter(
    expectedAfter: LocalDateTime,
    actual: LocalDateTime,
) {
    assertTrue(actual.isAfter(expectedAfter), "Forventet tidspunkt etter $expectedAfter, men var $actual")
}

fun assertAtLeast(
    expectedMinimum: Long,
    actual: Long,
) {
    assertTrue(actual >= expectedMinimum, "Forventet minst $expectedMinimum, men var $actual")
}

fun assertAtLeast(
    expectedMinimum: Int,
    actual: Int,
) {
    assertTrue(actual >= expectedMinimum, "Forventet minst $expectedMinimum, men var $actual")
}
