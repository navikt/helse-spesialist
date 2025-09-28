package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.Period
import java.time.YearMonth
import java.util.UUID
import kotlin.reflect.KClass

sealed interface GQLScalarType : GQLNamedInputType, GQLNamedOutputType {
    override fun toSDL(): String = "scalar $name"
    override fun toSelectionSet(allOutputTypes: Collection<GQLOutputType>): String = ""
}

class GQLCustomScalarType(override val name: String) : GQLScalarType

class GQLDefaultScalarType(override val name: String) : GQLScalarType

val scalarTypes = mapOf(
    defaultScalarType(klass = Int::class, name = "Int"),
    defaultScalarType(klass = Float::class, name = "Float"),
    defaultScalarType(klass = String::class, name = "String"),
    defaultScalarType(klass = Boolean::class, name = "Boolean"),

    // Primitives
    customScalarType(klass = Byte::class, name = "Byte"),
    customScalarType(klass = Short::class, name = "Short"),
    customScalarType(klass = Long::class, name = "Long"),
    customScalarType(klass = Double::class, name = "Double"),
    customScalarType(klass = Char::class, name = "Char"),

    // java.math
    customScalarType(klass = BigDecimal::class, name = "BigDecimal"),

    // java.util
    customScalarType(klass = UUID::class, name = "UUID"),

    // java.time
    customScalarType(klass = Duration::class, name = "Duration"),
    customScalarType(klass = Instant::class, name = "Instant"),
    customScalarType(klass = LocalDate::class, name = "LocalDate"),
    customScalarType(klass = LocalTime::class, name = "LocalTime"),
    customScalarType(klass = LocalDateTime::class, name = "LocalDateTime"),
    customScalarType(klass = MonthDay::class, name = "MonthDay"),
    customScalarType(klass = Period::class, name = "Period"),
    customScalarType(klass = YearMonth::class, name = "YearMonth"),
)

private fun defaultScalarType(klass: KClass<*>, name: String): Pair<KClass<*>, GQLScalarType> =
    klass to GQLDefaultScalarType(name)

private fun customScalarType(klass: KClass<*>, name: String): Pair<KClass<*>, GQLScalarType> =
    klass to GQLCustomScalarType(name)
