package no.nav.helse.mediator

import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun JsonNode.asBigDecimal(): BigDecimal = BigDecimal(asString())

inline fun <reified T : Enum<T>> JsonNode.asEnum(): T = enumValueOf<T>(asString())

fun JsonNode.asLocalDate(): LocalDate = LocalDate.parse(asString())

fun JsonNode?.asLocalDateOrNull(): LocalDate? = this?.takeIf { it.isString }?.asLocalDate()

fun JsonNode.asLocalDateTime(): LocalDateTime = LocalDateTime.parse(asString())

fun JsonNode?.asLocalDateTimeOrNull(): LocalDateTime? = this?.takeIf { it.isString }?.asLocalDateTime()

fun JsonNode.asUUID(): UUID = UUID.fromString(asString())

fun JsonNode?.asUUIDOrNull(): UUID? = this?.takeIf { it.isString }?.asUUID()

fun JsonNode?.isMissingOrNull() = this == null || this.isNull || this.isMissingNode
