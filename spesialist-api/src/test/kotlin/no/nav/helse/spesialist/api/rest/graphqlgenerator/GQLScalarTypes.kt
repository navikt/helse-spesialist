package no.nav.helse.spesialist.api.rest.graphqlgenerator

import kotlin.reflect.KClass

sealed interface GQLScalarType : GQLNamedInputType, GQLNamedOutputType {
    override fun toSDL(): String = "scalar $name\n"
    override fun toSelectionSet(indentationLevel: Int, allOutputTypes: Collection<GQLOutputType>): String = ""
}

class GQLCustomScalarType(override val name: String) : GQLScalarType

class GQLDefaultScalarType(override val name: String) : GQLScalarType

val scalarTypes = mapOf(
    defaultScalarType(klass = kotlin.Int::class, name = "Int"),
    defaultScalarType(klass = kotlin.Float::class, name = "Float"),
    defaultScalarType(klass = kotlin.String::class, name = "String"),
    defaultScalarType(klass = kotlin.Boolean::class, name = "Boolean"),

    // Primitives
    customScalarType(klass = kotlin.Byte::class, name = "Byte"),
    customScalarType(klass = kotlin.Short::class, name = "Short"),
    customScalarType(klass = kotlin.Long::class, name = "Long"),
    customScalarType(klass = kotlin.Double::class, name = "Double"),
    customScalarType(klass = kotlin.Char::class, name = "Char"),

    // java.math
    customScalarType(klass = java.math.BigDecimal::class, name = "BigDecimal"),

    // java.util
    customScalarType(klass = java.util.UUID::class, name = "UUID"),

    // java.time
    customScalarType(klass = java.time.Duration::class, name = "Duration"),
    customScalarType(klass = java.time.Instant::class, name = "Instant"),
    customScalarType(klass = java.time.LocalDate::class, name = "LocalDate"),
    customScalarType(klass = java.time.LocalTime::class, name = "LocalTime"),
    customScalarType(klass = java.time.LocalDateTime::class, name = "LocalDateTime"),
    customScalarType(klass = java.time.MonthDay::class, name = "MonthDay"),
    customScalarType(klass = java.time.Period::class, name = "Period"),
    customScalarType(klass = java.time.YearMonth::class, name = "YearMonth"),
)

private fun defaultScalarType(klass: KClass<*>, name: String): Pair<KClass<*>, GQLScalarType> =
    klass to GQLDefaultScalarType(name)

private fun customScalarType(klass: KClass<*>, name: String): Pair<KClass<*>, GQLScalarType> =
    klass to GQLCustomScalarType(name)
