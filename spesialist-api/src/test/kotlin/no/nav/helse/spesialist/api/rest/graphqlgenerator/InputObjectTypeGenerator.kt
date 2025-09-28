package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLInputObjectType
import no.nav.helse.spesialist.api.rest.graphqlgenerator.types.GQLNamedType
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses

class InputObjectTypeGenerator(
    private val inputTypeGenerator: InputTypeGenerator,
    private val typeNameGenerator: TypeNameGenerator,
) {
    val inputObjectTypes = mutableMapOf<KClass<*>, GQLInputObjectType>()

    fun resolveOrGenerate(klass: KClass<*>): GQLInputObjectType =
        inputObjectTypes.getOrPut(klass) { generateInputObjectType(klass) }

    private fun generateInputObjectType(klass: KClass<*>): GQLInputObjectType {
        println("Genererer input type for $klass...")

        if (klass.superclasses.filterNot { it == Any::class }.isNotEmpty()) {
            error("Støtter ikke input-typer som arver av andre typer. Gjelder $klass.")
        }
        if (klass.isAbstract || klass.java.isInterface) {
            error("Støtter ikke abstrakt klasse / interface som input. Gjelder $klass.")
        }

        val fields = klass.declaredMemberProperties
            .filterNot { it.name == "__typename" }
            .onEach { it.name.assertNoÆØÅ() }
            .associate { it.name to inputTypeGenerator.resolveOrGenerate(it.returnType) }
        if (fields.isEmpty()) {
            error("Alle GraphQL-typer må ha minst ett felt. Gjelder $klass.")
        }

        return GQLInputObjectType(
            name = typeNameGenerator.generateGQLTypeName(klass, "Input"),
            fields = fields
        )
    }

    fun lookupGQLType(name: String): Pair<KClass<*>, GQLNamedType>? =
        inputObjectTypes.entries
            .find { (_, type) -> type.name == name }
            ?.toPair()
}
