package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLEnumType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import kotlin.reflect.KClass

class EnumTypeGenerator(
    private val typeNameGenerator: TypeNameGenerator
) {
    val enumTypes = mutableMapOf<KClass<*>, GQLEnumType>()

    fun isEnum(klass: KClass<*>): Boolean = klass.java.isEnum

    fun resolveOrGenerate(klass: KClass<*>): GQLEnumType =
        enumTypes.getOrPut(klass) { generateEnumType(klass) }

    private fun generateEnumType(klass: KClass<*>): GQLEnumType {
        println("Genererer enum-type for $klass...")
        check(isEnum(klass)) { error("$klass er ikke en enum-type") }
        return GQLEnumType(
            name = typeNameGenerator.generateGQLTypeName(klass, "Enum"),
            values = klass.java.enumConstants.map { it as Enum<*> }.map { it.name }.toSet()
        )
    }

    fun lookupGQLType(name: String): Pair<KClass<*>, GQLNamedType>? =
        enumTypes.entries
            .find { (_, type) -> type.name == name }
            ?.toPair()

    fun allTypes(): Collection<GQLEnumType> = enumTypes.values
}
