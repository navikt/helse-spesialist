package no.nav.helse.spesialist.api.rest.graphqlgenerator

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

val scalarTypes = mapOf(
    scalarEntry(String::class, "String"),
    scalarEntry(Boolean::class, "Boolean"),
    scalarEntry(Int::class, "Int"),
    scalarEntry(Short::class, "Short"),
    scalarEntry(Long::class, "Long"),
    scalarEntry(Float::class, "Float"),
    scalarEntry(Double::class, "Double"),
    scalarEntry(BigDecimal::class, "BigDecimal"),
    scalarEntry(LocalDate::class, "LocalDate"),
    scalarEntry(LocalDateTime::class, "LocalDateTime"),
    scalarEntry(Instant::class, "Instant"),
    scalarEntry(UUID::class, "UUID")
)

private fun scalarEntry(klass: KClass<*>, name: String): Pair<KType, ScalarTypeDefinition> =
    klass.starProjectedType.withNullability(false) to ScalarTypeDefinition(name)
