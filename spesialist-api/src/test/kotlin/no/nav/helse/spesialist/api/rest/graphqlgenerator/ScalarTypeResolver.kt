package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLCustomScalarType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLDefaultScalarType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLScalarType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass

class ScalarTypeResolver {
    val customScalarTypes = mapOf(
        BigDecimal::class to GQLCustomScalarType(name = "BigDecimal"),
        LocalDate::class to GQLCustomScalarType(name = "LocalDate"),
        LocalDateTime::class to GQLCustomScalarType(name = "LocalDateTime"),
        UUID::class to GQLCustomScalarType(name = "UUID"),
    )

    fun isScalar(klass: KClass<*>): Boolean =
        defaultScalarTypes.containsKey(klass) || customScalarTypes.containsKey(klass)

    fun resolve(klass: KClass<*>): GQLScalarType =
        defaultScalarTypes[klass] ?: customScalarTypes[klass] ?: error("$klass er ikke en skalartype")

    private val booleanType = GQLDefaultScalarType("Boolean")
    private val intType = GQLDefaultScalarType("Int")
    private val floatType = GQLDefaultScalarType("Float")
    private val stringType = GQLDefaultScalarType("String")

    private val defaultScalarTypes = mapOf(
        Boolean::class to booleanType,
        Int::class to intType,
        Short::class to intType,
        Byte::class to intType,
        Double::class to floatType,
        Float::class to floatType,
        String::class to stringType,
        Char::class to stringType,
    )

    fun lookupGQLType(name: String): Pair<KClass<*>, GQLNamedType>? =
        (defaultScalarTypes.entries + customScalarTypes.entries)
            .find { (_, type) -> type.name == name }
            ?.toPair()

    fun allTypes(): Collection<GQLCustomScalarType> = customScalarTypes.values
}
