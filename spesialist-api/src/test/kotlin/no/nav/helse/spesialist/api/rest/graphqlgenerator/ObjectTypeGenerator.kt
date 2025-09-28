package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLObjectType
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses

class ObjectTypeGenerator(
    private val outputTypeGenerator: OutputTypeGenerator,
    private val typeNameGenerator: TypeNameGenerator,
) {
    val objectTypes = mutableMapOf<KClass<*>, GQLObjectType>()

    fun resolveOrGenerateObjectType(klass: KClass<*>): GQLObjectType =
        objectTypes.getOrPut(klass) { generateObjectType(klass) }

    private fun generateObjectType(klass: KClass<*>): GQLObjectType {
        println("Genererer object-type for $klass...")
        return GQLObjectType(
            name = typeNameGenerator.generateGQLTypeName(klass, "Object"),
            implementedInterfaces = klass.superclasses.filterNot { it == Any::class }
                .map { outputTypeGenerator.interfaces.resolveOrGenerate(it) },
            fields = klass.declaredMemberProperties
                .filterNot { it.name == "__typename" }
                .onEach { it.name.assertNoÆØÅ() }
                .associate { it.name to outputTypeGenerator.resolveOrGenerate(it.returnType) }
                .ifEmpty {
                    error(
                        "Alle GraphQL-typer må ha minst ett felt. Hvis dette er en skalarverdi, legg den til i genereringen." +
                                " Gjelder $klass."
                    )
                }
        )
    }

    fun lookupGQLType(name: String): Pair<KClass<*>, GQLNamedType>? =
        objectTypes.entries
            .find { (_, type) -> type.name == name }
            ?.toPair()
}
