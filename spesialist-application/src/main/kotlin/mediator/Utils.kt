package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun JsonNode.asBigDecimal(): BigDecimal = BigDecimal(asText())

inline fun <reified T : Enum<T>> JsonNode.asEnum(): T = enumValueOf<T>(asText())

fun JsonNode.asLocalDate(): LocalDate = LocalDate.parse(asText())

fun JsonNode.asLocalDateTime(): LocalDateTime = LocalDateTime.parse(asText())

fun JsonNode.asUUID(): UUID = UUID.fromString(asText())
