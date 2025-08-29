package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID

inline fun <reified T : Enum<T>> JsonNode.asEnum(): T = enumValueOf<T>(asText())

fun JsonNode.asLocalDate(): LocalDate = LocalDate.parse(asText())

fun JsonNode.asUUID(): UUID = UUID.fromString(asText())
